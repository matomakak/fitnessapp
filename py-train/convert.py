import sys, os
import warnings

if not sys.warnoptions:
    warnings.simplefilter("ignore")

import argparse
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy import signal
import tensorflow as tf

tf.logging.set_verbosity(tf.logging.ERROR)
from math import ceil

parser = argparse.ArgumentParser(description="Process input CSV file of recorded activities and trains CNN.")
parser.add_argument('-i', '--input', dest="input", help="CSV formatted file", metavar="FILE", required=True)
parser.add_argument('-o', '--output', dest="output", help="Trained neuron network prefix name", metavar="FILE")
parser.add_argument('-m', '--model', dest="model",
                    help="Previously trained neuron network prefix name",
                    metavar="FILE")
parser.add_argument('--length', dest="sample_length",
                    help="Length of single sample, allowed values : \'max\',\'avg\' or ANY NUMBER")
parser.add_argument('-v', '--visualize', dest="visualize", help="Show plots of each recorded exercise",
                    action="store_true", default=False)
parser.add_argument('--image', dest='image', help="Show image of each recorded exercise chunk",
                    action="store_true", default=False)
parser.add_argument('-c', '--chunks', dest="chunks", help="Show plots of each recorded chunk of exercise",
                    action="store_true", default=False)
parser.add_argument('-f', '--fourier', dest="fourier", help="Show plots of each DFT used for period estimation",
                    action="store_true", default=False)
parser.add_argument('--estimations', dest="estimations", help="Show estimations(num. of records) of each activity",
                    action="store_true", default=False)

args = parser.parse_args()
if args.visualize is False and args.output is None and args.image is False:
    parser.error("-o is required when -v is not set.")
if args.model is not None and args.output is None:
    parser.error("-o is required when -m is not set.")
if args.chunks is True and args.visualize is False:
    parser.error("-v is required when -c is set.")
if args.fourier is True and args.visualize is False:
    parser.error("-v is required when -f is set.")
if args.sample_length is not None and args.output is None:
    parser.error("-o is required when --length is set.")

if args.sample_length != 'max' and args.sample_length != 'avg':
    try:
        i = int(args.sample_length)
    except ValueError:
        parser.error("allowed values for --length: \'max\',\'avg\' or ANY NUMBER")


def plot_activity_axis(ax, x, y, title, estimation=None):
    ax.plot(x, y)
    # ax.set_title(title)
    ax.xaxis.set_visible(False)
    ax.set_ylim([min(y) - np.std(y), max(y) + np.std(y)])
    ax.set_xlim([min(x), max(x)])
    ax.grid(True)
    if estimation:
        for x in range(0, len(x), int(len(x) / estimation) + 1):
            ax.axvline(x=x, color='r')


def plot_activity(name, x, y, z, estimation=None):
    fig, (ax0, ax1, ax2) = plt.subplots(nrows=3, figsize=(15, 10), sharex=True)
    length = len(x)
    plot_activity_axis(ax0, range(length), x, 'x-axis', estimation)
    plot_activity_axis(ax1, range(length), y, 'y-axis', estimation)
    plot_activity_axis(ax2, range(length), z, 'z-axis', estimation)
    plt.subplots_adjust(hspace=0.2)
    fig.suptitle(name)
    plt.subplots_adjust(top=0.90)
    # plt.savefig("images/" + name + ".png")
    plt.show()


def plot_fft_axis(subplot, axis, id):
    W = np.fft.fft(axis)
    length = axis.size
    freq = np.fft.fftfreq(axis.size, 1)
    x = 1.0 / freq[:ceil(axis.size / 2)]

    # subplot.yaxis.set_visible(False)
    subplot.set_xlim(0, 80)
    subplot.plot(length / x, abs(W[:ceil(length / 2)]))
    subplot.scatter([length / (1 / abs(freq[id])), ], [np.abs(W[id]), ], s=100, color='r')


def plot_fft(name, X, idx, Y, idy, Z, idz):
    fig, (ax, ay, az) = plt.subplots(nrows=3, figsize=(15, 10), sharex=True)

    plot_fft_axis(ax, X, idx)
    plot_fft_axis(ay, Y, idy)
    plot_fft_axis(az, Z, idz)

    plt.subplots_adjust(hspace=0.4)
    fig.suptitle(name)
    plt.show()


def normalize_axis(input):
    mu = np.mean(input, axis=0)
    sigma = np.std(input, axis=0)
    return (input - mu) / sigma


def estimate_period(df):
    if df['activity'].values[0].lower() == "none":
        return 0
    X = normalize_axis(df['x-axis']).values
    Y = normalize_axis(df['y-axis']).values
    Z = normalize_axis(df['z-axis']).values

    # Look for the longest signal that is "loud"
    threshold = 225
    try:
        idx = np.concatenate([[0], np.where(abs(np.fft.fft(X)) > threshold)[0]])[-1]
        idy = np.concatenate([[0], np.where(abs(np.fft.fft(Y)) > threshold)[0]])[-1]
        idz = np.concatenate([[0], np.where(abs(np.fft.fft(Z)) > threshold)[0]])[-1]
    except:
        print("problem with " + df['activity'] + " -- " + df['timestamp'])
        exit(1)

    if args.fourier:
        plot_fft(df['activity'].iloc[0], X, idx, Y, idy, Z, idz)

    freq = np.fft.fftfreq(X.size, 1)

    estimations = [X.size / (1 / abs(freq[idx])), Y.size / (1 / abs(freq[idy])), Z.size / (1 / abs(freq[idz]))]
    est_filtered = [x for x in estimations if 3 < x < 21]
    return 0 if len(est_filtered) < 1 else ceil(sum(est_filtered) / len(est_filtered))


def filter_and_estimate(data):
    avg_max = 0
    max_count = 0
    all_max = 0
    filtered_activities = []
    indexes = [0] + list(idx for idx, (i, j) in enumerate(zip(data['timestamp'], data['timestamp'][1:]), 1) if
                         (i + 1000 < j) or (i - 1000 > j))
    for i in range(len(indexes)):
        start = 0 if i == 0 else indexes[i] + 1
        if i + 1 < len(indexes):
            end = indexes[i + 1]
        else:
            end = -1
        exercise = data.iloc[start:end]
        filtered = exercise[
            (exercise['timestamp'] < (exercise.iloc[-1]['timestamp'] - 2700)) &
            (exercise['timestamp'] > (exercise.iloc[0]['timestamp'] + 300))
            ]
        estimated_periods = estimate_period(filtered)
        estimation = 0 if estimated_periods < 1 else estimated_periods
        max_est = 1 if estimated_periods < 1 else ceil(len(filtered) / estimated_periods)
        all_max = max_est if all_max < max_est else all_max
        exercise_name = exercise.iloc[0]['activity']

        if max_est > 1:
            if args.estimations:
                print(str(max_est) + "--" + exercise_name)
            avg_max += max_est
            max_count += 1

        if exercise_name in filtered_activities:
            filtered_activities[exercise_name] = \
                (filtered_activities[exercise_name][0].append(filtered),
                 filtered_activities[exercise_name][1] + estimation)
        else:
            filtered_activities.append((exercise_name, filtered, estimation))

    # FIXME SHOWING AVERAGE
    print("\n\nAVERAGE --" + str(avg_max / max_count))
    if args.sample_length is None:
        length = avg_max
    elif args.sample_length == 'max':
        length = all_max
    elif args.sample_length == 'avg':
        length = avg_max // max_count
    else:
        length = args.sample_length
    return length, filtered_activities


def read_data_and_filter(file_path):
    column_names = ['activity', 'timestamp', 'x-axis', 'y-axis', 'z-axis']
    data = pd.read_csv(file_path, header=None, names=column_names, comment=';')
    data.dropna(axis=0, how='any', inplace=True)
    return filter_and_estimate(data)


def plot_activities_and_exit(max_len, activities_list):
    for activity, filtered, estimation in activities_list:
        plot_activity(activity + "--" + str(estimation) + "--" + str(filtered['timestamp'].values[0]),
                      filtered['x-axis'], filtered['y-axis'], filtered['z-axis'], estimation)
        if args.chunks:
            chunks = estimation if estimation > 0 else ceil(len(filtered) / max_len)
            for chunk in np.array_split(filtered, chunks):
                plot_activity(activity, chunk['x-axis'], chunk['y-axis'], chunk['z-axis'])
    exit(0)


def plot_images_and_exit(length, activities_list):
    l_segments, l_labels = segment_signal(ceil(np.sqrt(length)) ** 2, activities_list)
    f = lambda x: np.abs(x)
    l_segments = f(l_segments)
    f1 = lambda x: x / np.max(x) * 255

    fig = plt.figure(figsize=(11, 11))
    columns = int(np.sqrt(l_segments.shape[0]))
    rows = ceil(l_segments.shape[0] / columns)

    ax = []
    for j in range(l_segments.shape[0]):
        l_segments[j] = f1(l_segments[j])
        img = l_segments[j].reshape(11, 11, 3).astype(np.uint8)
        ax.append(fig.add_subplot(rows, columns, j + 1))
    ax[-1].set_title(l_labels[j])
    ax[-1].axis('off')
    plt.imshow(img)
    plt.show()
    exit(0)


def segment_signal(max_len, filtered_activities):
    l_labels = np.empty(0)
    l_segments = np.empty((0, max_len, 3))

    for activity, filtered, estimation in filtered_activities:
        chunks = estimation if estimation > 0 else ceil(len(filtered) / max_len)
        for chunk in np.array_split(filtered, chunks):
            x = signal.resample(chunk['x-axis'].values, max_len)
            y = signal.resample(chunk['y-axis'].values, max_len)
            z = signal.resample(chunk['z-axis'].values, max_len)

            l_segments = np.vstack([l_segments, np.dstack([x, y, z])])
            l_labels = np.append(l_labels, activity)

    return l_segments, l_labels


def get_labels_file_content(labels, length):
    return "#SAMPLES#" + str(length) + "\n" + "\n".join(np.unique(labels))


length, activities = read_data_and_filter(args.input)

if args.image:
    plot_images_and_exit(length, activities)

if args.visualize:
    plot_activities_and_exit(length, activities)

MODEL_NAME = "./" + args.output
INPUT_NAME = "I"
LABEL_NAME = "Y"
C_NAME = "c"
LOSS_NAME = "Neg"
ACCURACY_NAME = "accuracy"
OUTPUT_NAME = "O"
checkpoint_path = MODEL_NAME + ".ckpt"

training_epochs = 4
batch_size = 10


def finalize_learning(svr, sess, input, output, labels, length):
    svr.save(sess, save_path=checkpoint_path)
    converter = tf.contrib.lite.TFLiteConverter.from_session(sess, [input],
                                                             [output])
    tflite_model = converter.convert()
    open(MODEL_NAME + ".tflite", 'wb').write(tflite_model)
    open(MODEL_NAME + ".labels", 'w').write(get_labels_file_content(labels, length))


if args.model is not None:
    model_suffix = ':0'
    with tf.Session() as session:
        head, tail = os.path.split(args.model)
        saver = tf.train.import_meta_graph(tail + ".meta")
        if not head:
            head = "./"
        saver.restore(session, tf.train.latest_checkpoint(head))

        graph = tf.get_default_graph()
        c = graph.get_tensor_by_name(C_NAME + model_suffix)
        Y = graph.get_tensor_by_name(LABEL_NAME + model_suffix)
        X = graph.get_tensor_by_name(INPUT_NAME + model_suffix)
        y_ = graph.get_tensor_by_name(OUTPUT_NAME + model_suffix)
        loss = graph.get_tensor_by_name(LOSS_NAME + model_suffix)
        optimizer = tf.train.GradientDescentOptimizer(learning_rate=0.0001).minimize(loss)
        accuracy = graph.get_tensor_by_name(ACCURACY_NAME + model_suffix)

        length = X.shape.dims[2].value if args.sample_length is None else args.sample_length
        segments, labels_full = segment_signal(length, activities)
        reshaped = segments.reshape(len(segments), 1, length, 3)
        labels = np.asarray(pd.get_dummies(labels_full), dtype=np.int8)

        train_test_split = np.random.rand(len(reshaped)) < 0.70
        train_x = reshaped[train_test_split]
        train_y = labels[train_test_split]
        test_x = reshaped[~train_test_split]
        test_y = labels[~train_test_split]
        total_batches = train_x.shape[0] // batch_size

        for epoch in range(training_epochs):
            for b in range(total_batches):
                offset = (b * batch_size) % (train_y.shape[0] - batch_size)
                batch_x = train_x[offset:(offset + batch_size), :, :, :]
                batch_y = train_y[offset:(offset + batch_size), :]
                _, c = session.run([optimizer, loss],
                                   feed_dict={X: batch_x, Y: batch_y})
            print("Epoch: ", epoch + 1, " Training Loss: ", c, " Training Accuracy: ",
                  session.run(accuracy, feed_dict={X: train_x, Y: train_y}))
        print("Testing Accuracy:", session.run(accuracy, feed_dict={X: test_x, Y: test_y}))
        finalize_learning(saver, session, X, y_, labels_full, length)
        exit(0)

segments, labels_full = segment_signal(length, activities)
reshaped = segments.reshape(len(segments), 1, length, 3)
labels = np.asarray(pd.get_dummies(labels_full), dtype=np.int8)

train_test_split = np.random.rand(len(reshaped)) < 0.70
train_x = reshaped[train_test_split]
train_y = labels[train_test_split]
test_x = reshaped[~train_test_split]
test_y = labels[~train_test_split]
##############################################################################
num_labels = np.unique(labels_full).size
num_channels = 3
kernel_size = 60
depth = 60
num_hidden = 1000
total_batches = train_x.shape[0] // batch_size


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


##############################################################################
X = tf.placeholder(tf.float32, shape=[None, 1, length, num_channels], name=INPUT_NAME)
Y = tf.placeholder(tf.float32, shape=[None, num_labels], name=LABEL_NAME)
c = apply_depthwise_conv(X, kernel_size, num_channels, depth, None)
p = apply_max_pool(c, 20, 2)
c = apply_depthwise_conv(p, 6, depth * num_channels, depth // 10, C_NAME)
shape = c.get_shape().as_list()
c_flat = tf.reshape(c, [-1, shape[1] * shape[2] * shape[3]])
f_weights_l1 = weight_variable([shape[1] * shape[2] * depth * num_channels * (depth // 10), num_hidden])
f_biases_l1 = bias_variable([num_hidden])
f = tf.nn.tanh(tf.add(tf.matmul(c_flat, f_weights_l1), f_biases_l1))
out_weights = weight_variable([num_hidden, num_labels])
out_biases = bias_variable([num_labels])
y_ = tf.nn.softmax(tf.matmul(f, out_weights) + out_biases, name=OUTPUT_NAME)
#############################################################################
loss = -tf.reduce_sum(Y * tf.log(y_), name=LOSS_NAME)
optimizer = tf.train.GradientDescentOptimizer(learning_rate=0.0001).minimize(loss)
correct_prediction = tf.equal(tf.argmax(y_, 1), tf.argmax(Y, 1))
accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32), name=ACCURACY_NAME)

init_op = tf.global_variables_initializer()
saver = tf.train.Saver()
with tf.Session() as session:
    session.run(init_op)
    for epoch in range(training_epochs):
        for b in range(total_batches):
            offset = (b * batch_size) % (train_y.shape[0] - batch_size)
            batch_x = train_x[offset:(offset + batch_size), :, :, :]
            batch_y = train_y[offset:(offset + batch_size), :]
            _, c = session.run([optimizer, loss], feed_dict={X: batch_x, Y: batch_y})
        print("Epoch: ", epoch + 1, " Training Loss: ", c, " Training Accuracy: ",
              session.run(accuracy, feed_dict={X: train_x, Y: train_y}))
    print("Testing Accuracy:", session.run(accuracy, feed_dict={X: test_x, Y: test_y}))
    # tf.train.write_graph(session.graph_def, '.', "Users/Martin/Desktop/model.pbtxt", as_text=True)
    finalize_learning(saver, session, X, y_, labels_full, length)
exit(0)
###########################################
# FREEZING AND RECOVERING + SAVING GRAPH #
###########################################
# from tensorflow.python.tools import freeze_graph
#
# input_graph_path = MODEL_NAME + '.pbtxt'
# input_saver_def_path = ""
# input_binary = False
# output_node_names = OUTPUT_NAME
# restore_op_name = "save/restore_all"
# filename_tensor_name = "save/Const:0"
# output_frozen_graph_name = MODEL_NAME + '_frozen' + '.pb'
# clear_devices = True
#
# freeze_graph.freeze_graph(input_graph_path, input_saver_def_path, input_binary, checkpoint_path, output_node_names,
#                           restore_op_name, filename_tensor_name, output_frozen_graph_name, clear_devices, "")
##########
# TFLITE #
##########
# converter = tf.contrib.lite.TFLiteConverter.from_frozen_graph(MODEL_NAME + '_frozen' + '.pb', ["I"],
#                                                       [OUTPUT_NAME])
# tflite_model = converter.convert()
# open(MODEL_NAME + "converted.tflite", "wb").write(tflite_model)
