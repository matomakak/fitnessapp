#  Copyright (c) 2019. Martin Hlavačka

import os

import numpy as np
import pandas as pd
import tensorflow as tf
from math import ceil
from scipy import signal

import globals as glb

tf.logging.set_verbosity(tf.logging.ERROR)


class NEURAL_NETWORK:
    """
    Class providing functionality for re/training of neural network with its definition, initialization and proper data
    exporting possibilities
    """
    def __init__(self, retraining, length, activities):
        """
        Initializes class with dummy values
        :param retraining: identifies if already existing trained model was provided
        :param length: size of each sample within all datasets
        :param activities: list of all datasets
        """
        self.MODEL_NAME = glb.output_name()
        self.length = length
        self.activities = activities

        self.session = tf.Session()

        self.total_batches = None
        self.train_x = None
        self.train_y = None
        self.test_x = None
        self.test_y = None

        self.optimizer = None
        self.loss = None
        self.accuracy = None

        self.X = None
        self.Y = None
        self.c = None
        self.y_ = None

        self.saver = None
        self.labels_full = None

        if retraining:
            self.initialize_retrain()
        else:
            self.initialize_train()

    def initialize_train(self):
        """
        Provides initialization of CNN within first training
        """
        segments, self.labels_full = segment_signal(self.length, self.activities)
        reshaped = segments.reshape(len(segments), 1, self.length, glb.NUM_CHANNELS)
        labels = np.asarray(pd.get_dummies(self.labels_full), dtype=np.int8)

        train_test_split = np.random.rand(len(reshaped)) < 0.70
        self.train_x = reshaped[train_test_split]
        self.train_y = labels[train_test_split]
        self.test_x = reshaped[~train_test_split]
        self.test_y = labels[~train_test_split]

        num_labels = np.unique(self.labels_full).size
        kernel_size = 60
        depth = 60
        num_hidden = 1000
        self.total_batches = self.train_x.shape[0] // glb.BATCH_SIZE

        self.X = tf.placeholder(tf.float32, shape=[None, 1, self.length, glb.NUM_CHANNELS], name=glb.INPUT_NAME)
        self.Y = tf.placeholder(tf.float32, shape=[None, num_labels], name=glb.LABEL_NAME)
        self.c = apply_depthwise_conv(self.X, kernel_size, glb.NUM_CHANNELS, depth, None)
        p = apply_max_pool(self.c, 20, 2)
        self.c = apply_depthwise_conv(p, 6, depth * glb.NUM_CHANNELS, depth // 10, glb.C_NAME)
        shape = self.c.get_shape().as_list()
        c_flat = tf.reshape(self.c, [-1, shape[1] * shape[2] * shape[3]])
        f_weights_l1 = weight_variable([shape[1] * shape[2] * depth * glb.NUM_CHANNELS * (depth // 10), num_hidden])
        f_biases_l1 = bias_variable([num_hidden])
        f = tf.nn.tanh(tf.add(tf.matmul(c_flat, f_weights_l1), f_biases_l1))
        out_weights = weight_variable([num_hidden, num_labels])
        out_biases = bias_variable([num_labels])
        self.y_ = tf.nn.softmax(tf.matmul(f, out_weights) + out_biases, name=glb.OUTPUT_NAME)
        self.loss = -tf.reduce_sum(self.Y * tf.log(self.y_), name=glb.LOSS_NAME)
        self.optimizer = tf.train.GradientDescentOptimizer(learning_rate=glb.LEARNING_RATE).minimize(self.loss)
        correct_prediction = tf.equal(tf.argmax(self.y_, 1), tf.argmax(self.Y, 1))
        self.accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32), name=glb.ACCURACY_NAME)

        init_op = tf.global_variables_initializer()
        self.session.run(init_op)
        self.saver = tf.train.Saver()

    def initialize_retrain(self):
        """
        Reads already existing trained model which is going to be used for retraining, parses all required tensors
        """
        head, tail = os.path.split(self.MODEL_NAME)
        self.saver = tf.train.import_meta_graph(tail + ".meta")
        if not head:
            head = "./"
        self.saver.restore(self.session, tf.train.latest_checkpoint(head))

        graph = tf.get_default_graph()
        self.c = graph.get_tensor_by_name(glb.C_NAME + glb.RETRAIN_MODEL_SUFFIX)
        self.Y = graph.get_tensor_by_name(glb.LABEL_NAME + glb.RETRAIN_MODEL_SUFFIX)
        self.X = graph.get_tensor_by_name(glb.INPUT_NAME + glb.RETRAIN_MODEL_SUFFIX)
        self.y_ = graph.get_tensor_by_name(glb.OUTPUT_NAME + glb.RETRAIN_MODEL_SUFFIX)
        self.loss = graph.get_tensor_by_name(glb.LOSS_NAME + glb.RETRAIN_MODEL_SUFFIX)
        self.optimizer = tf.train.GradientDescentOptimizer(learning_rate=glb.LEARNING_RATE).minimize(self.loss)
        self.accuracy = graph.get_tensor_by_name(glb.ACCURACY_NAME + glb.RETRAIN_MODEL_SUFFIX)

        self.length = self.X.shape.dims[2].value if glb.sample_length() is None else glb.sample_length()
        segments, self.labels_full = segment_signal(self.length, self.activities)
        reshaped = segments.reshape(len(segments), 1, self.length, glb.NUM_CHANNELS)
        labels = np.asarray(pd.get_dummies(self.labels_full), dtype=np.int8)

        train_test_split = np.random.rand(len(reshaped)) < 0.70
        self.train_x = reshaped[train_test_split]
        self.train_y = labels[train_test_split]
        self.test_x = reshaped[~train_test_split]
        self.test_y = labels[~train_test_split]
        self.total_batches = self.train_x.shape[0] // glb.BATCH_SIZE

    def train(self):
        """
        Training process allowing to go through whole process of training and validating neural network with provided
        data
        """
        if glb.export_graph():
            writer = tf.summary.FileWriter(glb.EXPORT_GRAPH, self.session.graph)
        for epoch in range(glb.TRAINING_EPOCHS):
            for b in range(self.total_batches):
                offset = (b * glb.BATCH_SIZE) % (self.train_y.shape[0] - glb.BATCH_SIZE)
                batch_x = self.train_x[offset:(offset + glb.BATCH_SIZE), :, :, :]
                batch_y = self.train_y[offset:(offset + glb.BATCH_SIZE), :]
                _, c = self.session.run([self.optimizer, self.loss], feed_dict={self.X: batch_x, self.Y: batch_y})
            print("Epoch: ", epoch + 1, " Training Loss: ", self.c, " Training Accuracy: ",
                  self.session.run(self.accuracy, feed_dict={self.X: self.train_x, self.Y: self.train_y}))
        print("Testing Accuracy:",
              self.session.run(self.accuracy, feed_dict={self.X: self.test_x, self.Y: self.test_y}))
        if glb.export_graph():
            writer.close()
        self.finalize()

    def finalize(self):
        """
        Provides functionality to exports tensorflow checkpoint file, tensorflow lite file and metadata file for
        detection app purposes
        """
        self.saver.save(self.session, save_path=self.MODEL_NAME + ".ckpt")
        converter = tf.contrib.lite.TFLiteConverter.from_session(self.session, [self.X], [self.y_])
        open('./' + self.MODEL_NAME + ".tflite", 'wb').write(converter.convert())
        open('./' + self.MODEL_NAME + ".labels", 'w').write(self.get_labels_file_content())
        self.session.close()

    def get_labels_file_content(self):
        """
        Creates content for metadata file
        :return: string containing information about sample size used and all exercises ordered properly within training
        """
        return "#SAMPLES#" + str(self.length) + "\n" + "\n".join(np.unique(self.labels_full))

    def __del__(self):
        self.session.close()


def weight_variable(shape):
    initial = tf.truncated_normal(shape, stddev=0.1)
    return tf.Variable(initial)


def bias_variable(shape):
    initial = tf.constant(0.0, shape=shape)
    return tf.Variable(initial)


def depthwise_conv2d(x, W):
    return tf.nn.depthwise_conv2d(x, W, [1, 1, 1, 1], padding='VALID')


def apply_depthwise_conv(x, kernel_size, num_channels, depth, name):
    weights = weight_variable([1, kernel_size, num_channels, depth])
    biases = bias_variable([depth * num_channels])
    return tf.nn.relu(tf.add(depthwise_conv2d(x, weights), biases), name=name)


def apply_max_pool(x, kernel_size, stride_size):
    return tf.nn.max_pool(x, ksize=[1, 1, kernel_size, 1], strides=[1, 1, stride_size, 1], padding='VALID')


def segment_signal(max_len, filtered_activities):
    """
    Parses and prepares data from list of activities to CNN readable format
    :param max_len: size of single sample
    :param filtered_activities: list of activities
    :return: tuple of datasets and labels in proper array formatting
    """
    l_labels = np.empty(0)
    l_segments = np.empty((0, max_len, 3))

    for activity, filtered, estimation in filtered_activities:
        if glb.is_auto_filter():
            chunks = estimation if estimation > 0 else ceil(len(filtered) / max_len)
        else:
            chunks = estimation
        for chunk in np.array_split(filtered, chunks):
            x = chunk[glb.X_AXIS].values
            y = chunk[glb.Y_AXIS].values
            z = chunk[glb.Z_AXIS].values
            if len(x) > max_len:
                x = signal.resample(x, max_len)
                y = signal.resample(y, max_len)
                z = signal.resample(z, max_len)
            else:
                x = np.pad(x, (0, max_len - len(x)), mode='constant', constant_values=0)
                y = np.pad(y, (0, max_len - len(y)), mode='constant', constant_values=0)
                z = np.pad(z, (0, max_len - len(z)), mode='constant', constant_values=0)

            l_segments = np.vstack([l_segments, np.dstack([x, y, z])])
            l_labels = np.append(l_labels, activity)

    return l_segments, l_labels
