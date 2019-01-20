import sys
import warnings

if not sys.warnoptions:
    warnings.simplefilter("ignore")

import argparse
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy import stats, signal
import tensorflow as tf

tf.logging.set_verbosity(tf.logging.ERROR)
from math import ceil

# %matplotlib inline
# plt.style.use('ggplot')

parser = argparse.ArgumentParser(description='Process input CSV file of recorded activities and trains CNN.')
parser.add_argument("-i", "--input", dest="input", help="CSV formatted file", metavar="FILE", required=True)
parser.add_argument("-o", "--output", dest="output", help="Trained neuron network prefix name", metavar="FILE")
parser.add_argument("-v", "--visualize", dest="visualize", help="Show plots of each recorded exercise",
                    action="store_true", default=False)
parser.add_argument("--image", dest="image", help="Show image of each recorded exercise chunk",
                    action="store_true", default=False)
parser.add_argument("-c", "--chunks", dest="chunks", help="Show plots of each recorded chunk of exercise",
                    action="store_true", default=False)
parser.add_argument("-f", "--fourier", dest="fourier", help="Show plots of each DFT used for period estimation",
                    action="store_true", default=False)
args = parser.parse_args()
if args.visualize is False and args.output is None and args.image is False:
    parser.error('-o is required when -v is not set.')
if args.chunks is True and args.visualize is False:
    parser.error('-v is required when -c is set.')
if args.fourier is True and args.visualize is False:
    parser.error('-v is required when -f is set.')


def feature_normalize(to_normalize):
    mu = np.mean(to_normalize, axis=0)
    sigma = np.std(to_normalize, axis=0)
    return (to_normalize - mu) / sigma


def plot_axis(ax, x, y, title):
    ax.plot(x, y)
    ax.set_title(title)
    ax.xaxis.set_visible(False)
    ax.set_ylim([min(y) - np.std(y), max(y) + np.std(y)])
    ax.set_xlim([min(x), max(x)])
    ax.grid(True)


def plot_activity(name, x, y, z):
    fig, (ax0, ax1, ax2) = plt.subplots(nrows=3, figsize=(15, 10), sharex=True)
    length = len(x)
    plot_axis(ax0, range(length), x, 'x-axis')
    plot_axis(ax1, range(length), y, 'y-axis')
    plot_axis(ax2, range(length), z, 'z-axis')
    plt.subplots_adjust(hspace=0.2)
    fig.suptitle(name)
    plt.subplots_adjust(top=0.90)
    plt.show()


def plot_fft(name, X, idx, Y, idy, Z, idz):
    lngth = X.size
    W_x = np.fft.fft(X)
    W_y = np.fft.fft(Y)
    W_z = np.fft.fft(Z)
    freq = np.fft.fftfreq(X.size, 1)
    x = 1.0 / freq[:ceil(X.size / 2)]

    fig, (ax, ay, az) = plt.subplots(nrows=3, figsize=(15, 10), sharex=True)

    ax.set_title('x-axis')
    ax.yaxis.set_visible(False)
    ax.set_xlim(0, 80)
    # plt.ylim(0, np.nanmax(x[x != np.inf]))
    ax.plot(lngth / x, abs(W_x[:ceil(lngth / 2)]))
    ax.scatter([lngth / (1 / abs(freq[idx])), ], [np.abs(W_x[idx]), ], s=100, color='r')

    ay.set_title('y-axis')
    ay.yaxis.set_visible(False)
    ay.set_xlim(0, 80)
    ay.plot(lngth / x, abs(W_y[:ceil(lngth / 2)]))
    ay.scatter([lngth / (1 / abs(freq[idy])), ], [np.abs(W_y[idy]), ], s=100, color='r')

    az.set_title('z-axis')
    az.yaxis.set_visible(False)
    az.set_xlim(0, 80)
    az.plot(lngth / x, abs(W_z[:ceil(lngth / 2)]))
    az.scatter([lngth / (1 / abs(freq[idz])), ], [np.abs(W_z[idz]), ], s=100, color='r')

    plt.subplots_adjust(hspace=0.4)
    plt.show()


def estimate_period(df):
    X = feature_normalize(df["x-axis"]).values
    Y = feature_normalize(df["y-axis"]).values
    Z = feature_normalize(df["z-axis"]).values

    freq = np.fft.fftfreq(X.size, 1)
    # Look for the longest signal that is "loud"
    threshold = 100
    idx = np.where(abs(np.fft.fft(X)) > threshold)[0][-1]
    idy = np.where(abs(np.fft.fft(Y)) > threshold)[0][-1]
    idz = np.where(abs(np.fft.fft(Z)) > threshold)[0][-1]

    if args.fourier:
        plot_fft(df["activity"].iloc[0], X, idx, Y, idy, Z, idz)

    estimations = [X.size / (1 / abs(freq[idx])), Y.size / (1 / abs(freq[idy])), Z.size / (1 / abs(freq[idz]))]
    est_filtered = [x for x in estimations if 3 < x < 21]
    return 0 if len(est_filtered) < 1 else ceil(sum(est_filtered) / len(est_filtered))


def filter_and_estimate(data):
    all_max = 0
    filtered_activities = dict()
    indexes = data.ne(data.shift()).apply(lambda x: x.index[x].tolist()).values[0]
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
        if exercise_name in filtered_activities:
            filtered_activities[exercise_name] = \
                (filtered_activities[exercise_name][0].append(filtered),
                 filtered_activities[exercise_name][1] + estimation)
        else:
            filtered_activities[exercise_name] = (filtered, estimation)
    return all_max, filtered_activities


def read_data_and_filter(file_path):
    column_names = ['activity', 'timestamp', 'x-axis', 'y-axis', 'z-axis']
    data = pd.read_csv(file_path, header=None, names=column_names, comment=';')
    data.dropna(axis=0, how='any', inplace=True)
    return filter_and_estimate(data)


def plot_activities_and_exit(max_len, filtered):
    for activity, (filtered, estimation) in filtered.items():
        plot_activity(activity, filtered['x-axis'], filtered['y-axis'], filtered['z-axis'])
        chunks = estimation if estimation > 0 else ceil(len(filtered) / max_len)
        if args.chunks:
            for chunk in np.array_split(filtered, chunks):
                plot_activity(activity, chunk['x-axis'], chunk['y-axis'], chunk['z-axis'])
    exit(0)


def plot_images_and_exit(length, activities):
    segments, labels = segment_signal(ceil(np.sqrt(length)) ** 2, activities)

    f = lambda x: np.abs(x)
    segments = f(segments)
    f1 = lambda x: x / np.max(x) * 255

    fig = plt.figure(figsize=(11, 11))
    columns = int(np.sqrt(segments.shape[0]))
    rows = ceil(segments.shape[0] / columns)

    ax = []
    for j in range(segments.shape[0]):
        segments[j] = f1(segments[j])
        img = segments[j].reshape(11, 11, 3).astype(np.uint8)
        ax.append(fig.add_subplot(rows, columns, j + 1))
        ax[-1].set_title(labels[j])
        ax[-1].axis('off')
        plt.imshow(img)

    plt.show()
    exit(0)


def segment_signal(max_len, filtered_activities):
    l_labels = np.empty(0)
    l_segments = np.empty((0, max_len, 3))

    for activity, (filtered, estimation) in filtered_activities.items():
        chunks = estimation if estimation > 0 else ceil(len(filtered) / max_len)
        for chunk in np.array_split(filtered, chunks):
            x = signal.resample(chunk["x-axis"].values, max_len)
            y = signal.resample(chunk["y-axis"].values, max_len)
            z = signal.resample(chunk["z-axis"].values, max_len)

            l_segments = np.vstack([l_segments, np.dstack([x, y, z])])
            l_labels = np.append(l_labels, activity)

    return l_segments, l_labels


length, activities = read_data_and_filter(args.input)

if args.image:
    plot_images_and_exit(length, activities)

if args.visualize:
    plot_activities_and_exit(length, activities)

segments, labels = segment_signal(length, activities)
reshaped = segments.reshape(len(segments), 1, length, 3)
labels = np.asarray(pd.get_dummies(labels), dtype=np.int8)

train_test_split = np.random.rand(len(reshaped)) < 0.70
train_x = reshaped[train_test_split]
train_y = labels[train_test_split]
test_x = reshaped[~train_test_split]
test_y = labels[~train_test_split]

MODEL_NAME = "./" + args.output


def weight_variable(shape):
    initial = tf.truncated_normal(shape, stddev=0.1)
    return tf.Variable(initial)


def bias_variable(shape):
    initial = tf.constant(0.0, shape=shape)
    return tf.Variable(initial)


def depthwise_conv2d(x, W):
    return tf.nn.depthwise_conv2d(x, W, [1, 1, 1, 1], padding='VALID')


def apply_depthwise_conv(x, kernel_size, num_channels, depth):
    weights = weight_variable([1, kernel_size, num_channels, depth])
    biases = bias_variable([depth * num_channels])
    return tf.nn.relu(tf.add(depthwise_conv2d(x, weights), biases))


def apply_max_pool(x, kernel_size, stride_size):
    return tf.nn.max_pool(x, ksize=[1, 1, kernel_size, 1],
                          strides=[1, 1, stride_size, 1], padding='VALID')


##############################################################################
num_labels = len(activities)
num_channels = 3
kernel_size = 60
depth = 60
num_hidden = 1000
training_epochs = 4
batch_size = 10
total_batches = train_x.shape[0] // batch_size
##############################################################################
X = tf.placeholder(tf.float32, shape=[None, 1, length, num_channels], name="I")
Y = tf.placeholder(tf.float32, shape=[None, num_labels])
c = apply_depthwise_conv(X, kernel_size, num_channels, depth)
p = apply_max_pool(c, 20, 2)
c = apply_depthwise_conv(p, 6, depth * num_channels, depth // 10)
shape = c.get_shape().as_list()
c_flat = tf.reshape(c, [-1, shape[1] * shape[2] * shape[3]])
f_weights_l1 = weight_variable([shape[1] * shape[2] * depth * num_channels * (depth // 10), num_hidden])
f_biases_l1 = bias_variable([num_hidden])
f = tf.nn.tanh(tf.add(tf.matmul(c_flat, f_weights_l1), f_biases_l1))
out_weights = weight_variable([num_hidden, num_labels])
out_biases = bias_variable([num_labels])
y_ = tf.nn.softmax(tf.matmul(f, out_weights) + out_biases, name="O")
#############################################################################
loss = -tf.reduce_sum(Y * tf.log(y_))
optimizer = tf.train.GradientDescentOptimizer(learning_rate=0.0001).minimize(loss)
correct_prediction = tf.equal(tf.argmax(y_, 1), tf.argmax(Y, 1))
accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

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
    # saver.save(session, save_path="Users/Martin/Desktop/model.ckpt")
    converter = tf.contrib.lite.TFLiteConverter.from_session(session, [X], [y_])
    tflite_model = converter.convert()
    open(MODEL_NAME + ".tflite", "wb").write(tflite_model)

exit(0)

############################################
# #FREEZING AND RECOVERING + SAVING GRAPH# #
############################################

from tensorflow.python.tools import freeze_graph

input_graph_path = MODEL_NAME + '.pbtxt'
checkpoint_path = MODEL_NAME + '.ckpt'
input_saver_def_path = ""
input_binary = False
output_node_names = "O"
restore_op_name = "save/restore_all"
filename_tensor_name = "save/Const:0"
output_frozen_graph_name = MODEL_NAME + '_frozen' + '.pb'
clear_devices = True

freeze_graph.freeze_graph(input_graph_path, input_saver_def_path, input_binary, checkpoint_path, output_node_names,
                          restore_op_name, filename_tensor_name, output_frozen_graph_name, clear_devices, "")
##########
# TFLITE #
##########
converter = tf.lite.TFLiteConverter.from_frozen_graph(MODEL_NAME + '_frozen' + '.pb', ["I"], ["O"])
tflite_model = converter.convert()
open(MODEL_NAME + "converted.tflite", "wb").write(tflite_model)
