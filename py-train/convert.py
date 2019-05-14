import sys
import warnings

if not sys.warnoptions:
    warnings.simplefilter("ignore")

import json
import pandas as pd
from math import ceil
import matplotlib.pyplot as plt

from clickable_plot import ClickablePlot
from cnn import NEURAL_NETWORK, segment_signal
from globals import *

initialize_and_validate_globals()


def plot_activity_axis(ax, x, y, estimation=None):
    """
    Plots 1 axis within 3 layout graph, possibility to append estimated chunks number in data within title
    :param ax:
    :param x:
    :param y:
    :param estimation:
    :return:
    """
    ax.plot(x, y)
    ax.xaxis.set_visible(False)
    ax.set_ylim([min(y) - np.std(y), max(y) + np.std(y)])
    ax.set_xlim([min(x), max(x)])
    ax.grid(True)
    if estimation:
        if is_auto_filter():
            estimation = range(0, len(x), int(len(x) / estimation) + 1)
        for x in estimation:
            ax.axvline(x=x, color='r')


def plot_activity(name, x, y, z, estimation=None, type=None):
    """
    Plots 3 layout graph for 1 dataset of 1 exercise with possibility to create clickable graph with extended
    functionality
    :param name: exercise name
    :param x: array of x-axis values
    :param y: array of y-axis values
    :param z: array of z-axis values
    :param estimation: estimated chunks in dataset
    :param type: type of graph (if specified using ClickablePlot)
    :return: if clickable plot - array of data chunks information
    """
    fig, (ax0, ax1, ax2) = plt.subplots(nrows=3, figsize=(15, 10), sharex=True)
    length = len(x)
    plot_activity_axis(ax0, range(length), x, estimation)
    plot_activity_axis(ax1, range(length), y, estimation)
    plot_activity_axis(ax2, range(length), z, estimation)
    plt.subplots_adjust(hspace=0.2)
    fig.suptitle(name)
    plt.subplots_adjust(top=0.90)
    if type:
        return ClickablePlot(fig, [ax0, ax1, ax2], type).wait_for_finish()
    else:
        # plt.savefig("images/" + name + ".png")
        plt.show()


def plot_fft_axis(subplot, axis, id):
    """
    Plots axis for DFT used within chunks estimation
    :param id: found peak (estimation)
    """
    W = np.fft.fft(axis)
    length = axis.size
    freq = np.fft.fftfreq(axis.size, 1)
    x = 1.0 / freq[:ceil(axis.size / 2)]
    # subplot.yaxis.set_visible(False)
    subplot.set_xlim(0, 80)
    subplot.plot(length / x, abs(W[:ceil(length / 2)]))
    subplot.scatter([length / (1 / abs(freq[id])), ], [np.abs(W[id]), ], s=100, color='r')


def plot_fft(name, X, idx, Y, idy, Z, idz):
    """
    Plots 3 layout DFT graph showing estimation within 1 dataset of 1 exercise
    :param name: exercise name
    :param X: data of x-axis
    :param idx: found peak in x-axis
    :param Y: data of y-axis
    :param idy: found peak in y-axis
    :param Z: data of z-axis
    :param idz: found peak in y-axis
    """
    fig, (ax, ay, az) = plt.subplots(nrows=3, figsize=(15, 10), sharex=True)

    plot_fft_axis(ax, X, idx)
    plot_fft_axis(ay, Y, idy)
    plot_fft_axis(az, Z, idz)

    plt.subplots_adjust(hspace=0.4)
    fig.suptitle(name)
    plt.show()


def normalize_axis(input):
    """
    Normalize array of data for DFT estimation purposes
    :param input: array of data
    :return: normalized array
    """
    mu = np.mean(input, axis=0)
    sigma = np.std(input, axis=0)
    return (input - mu) / sigma


def estimate_period(df):
    """
    Provides functionality for automatic estimation via DFT
    :param df: dataframe containing all columns for 1 dataset of 1 exercise
    :return: number of estimated repetitions in provided dataset
    """
    if df[NAME].values[0].lower() == "none":
        return 0
    X = normalize_axis(df[X_AXIS]).values
    Y = normalize_axis(df[Y_AXIS]).values
    Z = normalize_axis(df[Z_AXIS]).values

    # Look for the longest signal that is "loud"
    threshold = 225
    try:
        idx = np.concatenate([[0], np.where(abs(np.fft.fft(X)) > threshold)[0]])[-1]
        idy = np.concatenate([[0], np.where(abs(np.fft.fft(Y)) > threshold)[0]])[-1]
        idz = np.concatenate([[0], np.where(abs(np.fft.fft(Z)) > threshold)[0]])[-1]
    except:
        print("problem with " + df[NAME] + " -- " + df[TIMESTAMP])
        exit(1)

    if show_fourier():
        plot_fft(df[NAME].iloc[0], X, idx, Y, idy, Z, idz)

    freq = np.fft.fftfreq(X.size, 1)

    estimations = [X.size / (1 / abs(freq[idx])), Y.size / (1 / abs(freq[idy])), Z.size / (1 / abs(freq[idz]))]
    est_filtered = [x for x in estimations if 3 < x < 21]
    return 0 if len(est_filtered) < 1 else ceil(sum(est_filtered) / len(est_filtered))


def filter_and_estimate(data):
    """
    Provides basic funtionality for filtering in and out within datasts and uses either manual or automatic estimations
    of chunks{repetitions) within each dataset
    Also provides possibility to save manual filtering data inputed from user
    :param data: all datasets
    :return: tuple: identified max/avg size of one chunk, list of all datasets filtered
    """
    avg_max = 0
    max_count = 0
    all_max = 0
    filtered_activities = []
    indexes = [0] + list(idx for idx, (i, j) in enumerate(zip(data[TIMESTAMP], data[TIMESTAMP][1:]), 1) if
                         (i + 1000 < j) or (i - 1000 > j))
    for i in range(len(indexes)):
        start = 0 if i == 0 else indexes[i] + 1
        if i + 1 < len(indexes):
            end = indexes[i + 1]
        else:
            end = -1
        exercise = data.iloc[start:end]
        exercise[X_AXIS] = convolve1d(exercise[X_AXIS].values)
        exercise[Y_AXIS] = convolve1d(exercise[Y_AXIS].values)
        exercise[Z_AXIS] = convolve1d(exercise[Z_AXIS].values)

        exercise_name = exercise.iloc[0][NAME]

        if is_auto_filter():
            filtered = exercise[
                (exercise[TIMESTAMP] < (exercise.iloc[-1][TIMESTAMP] - 2700)) &
                (exercise[TIMESTAMP] > (exercise.iloc[0][TIMESTAMP] + 300))
                ]

            signal_pieces = estimate_period(filtered)
            estimation = 0 if signal_pieces < 1 else signal_pieces
            max_est = 1 if signal_pieces < 1 else ceil(len(filtered) / signal_pieces)
            if max_est > 1:
                avg_max += max_est
                max_count += 1
        else:
            found = None
            if filter_input():
                for entry in filter_input_data:
                    if (entry['info']['timestamp'] == int(exercise[TIMESTAMP].iloc[0])):
                        found = entry
                        break
            if found is not None:
                min_max = [found['filter']['min'], found['filter']['max']]
                filtered = exercise.iloc[min_max[0]:min_max[1]]
                estimation = found['chunks']
            else:
                min_max = plot_activity("FILTER START & FINISH", exercise[X_AXIS], exercise[Y_AXIS], exercise[Z_AXIS],
                                        type=ClickablePlot.Type.FILTER)
                filtered = exercise.iloc[min_max[0]:min_max[1]]
                estimation = plot_activity("SELECT CHUNKS", filtered[X_AXIS], filtered[Y_AXIS], filtered[Z_AXIS],
                                           type=ClickablePlot.Type.SPLIT)
                if export_filter():
                    filter_entry = {}
                    filter_entry['info'] = {'name': exercise_name, 'timestamp': int(exercise[TIMESTAMP].iloc[0])}
                    filter_entry['filter'] = {'min': min_max[0], 'max': min_max[1]}
                    filter_entry['chunks'] = estimation
                    filter_output_data.append(filter_entry)

            idxs = [0] + estimation + [len(filtered) - 1]
            diffs = [x - idxs[i - 1] for i, x in enumerate(idxs) if i > 0]
            max_est = max(diffs)
            avg_max += sum(diffs)
            max_count += len(diffs)

        if show_estimation():
            print(str(max_est) + "--" + exercise_name)

        all_max = max_est if all_max < max_est else all_max

        filtered_activities.append((exercise_name, filtered, estimation))

    if sample_length() is None or sample_length() == 'avg':
        print("\n\nAVERAGE LENGTH--" + str(avg_max / max_count))
        length = avg_max // max_count
    elif sample_length() == 'max':
        length = all_max
    else:
        length = sample_length()
    return length, filtered_activities


def read_data_and_filter(file_path):
    """
    Parses CSV data file and applies filter and estimation process
    :param file_path: name of input file
    :return: result from filter_and_estimate method
    """
    column_names = [NAME, TIMESTAMP, X_AXIS, Y_AXIS, Z_AXIS]
    data = pd.read_csv(file_path, header=None, names=column_names, comment=';')
    data.dropna(axis=0, how='any', inplace=True)
    return filter_and_estimate(data)


def plot_images_and_exit(length, activities_list):
    """
    Renders graphs for all datasets in provided list of exercises and exits
    :param length: size of one chunks
    :param activities_list: list of all exercise datasets
    """
    size = ceil(np.sqrt(length))
    l_segments, l_labels = segment_signal(size ** 2, activities_list)
    f = lambda x: np.abs(x)
    l_segments = f(l_segments)
    f1 = lambda x: x / np.max(x) * 255
    fig = plt.figure(figsize=(11, 11))

    ax = []
    k = 1
    for j in range(l_segments.shape[0]):
        if j > 0 and j % 25 == 0:
            k = 1
            plt.show()
            fig = plt.figure(figsize=(11, 11))
            ax = []
        l_segments[j] = f1(l_segments[j])
        ax.append(fig.add_subplot(5, 5, k))
        ax[-1].set_title(l_labels[j])
        ax[-1].axis('off')
        plt.imshow(l_segments[j].reshape(size, size, 3).astype(np.uint8))
        k += 1
    exit(0)


##############################################################################
filter_output_data = []
if filter_input():  # when requested prepares and read file containing manual filter data
    with open(filter_input()) as json_file:
        filter_input_data = json.load(json_file)
length, activities = read_data_and_filter(input_file())
if export_filter():  # when requested exports last manual data inputted within manual filtering/estimation
    with open(input_file + '.filtered_data', 'w') as outfile:
        json.dump(filter_output_data, outfile)

if show_image():
    plot_images_and_exit(length, activities)

if show_plot():
    for activity, filtered, estimation in activities:
        plot_activity(activity + "--" + str(filtered[TIMESTAMP].values[0]),
                      filtered[X_AXIS], filtered[Y_AXIS], filtered[Z_AXIS], estimation)
    plt.waitforbuttonpress()
    exit(0)

NEURAL_NETWORK(is_retraining(), length, activities).train()  # prepares and runs CNN
