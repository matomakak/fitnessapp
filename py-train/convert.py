import sys
import warnings

if not sys.warnoptions:
    warnings.simplefilter("ignore")

import pandas as pd
import numpy as np
from math import ceil
import matplotlib.pyplot as plt

from clickable_plot import ClickablePlot
from cnn import NEURAL_NETWORK, segment_signal
from globals import *

initialize_and_validate_globals()


def plot_activity_axis(ax, x, y, estimation=None):
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
    fig, (ax0, ax1, ax2) = plt.subplots(nrows=3, figsize=(15, 10), sharex=True)
    length = len(x)
    plot_activity_axis(ax0, range(length), x, estimation)
    plot_activity_axis(ax1, range(length), y, estimation)
    plot_activity_axis(ax2, range(length), z, estimation)
    plt.subplots_adjust(hspace=0.2)
    fig.suptitle(name + str(estimation) if estimation else "")
    plt.subplots_adjust(top=0.90)
    if type:
        return ClickablePlot(fig, [ax0, ax1, ax2], type).wait_for_finish()
    else:
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
            min_max = plot_activity("FILTER START & FINISH", exercise[X_AXIS], exercise[Y_AXIS], exercise[Z_AXIS],
                                    type=ClickablePlot.Type.FILTER)
            filtered = exercise.iloc[min_max[0]:min_max[1]]
            estimation = plot_activity("SELECT CHUNKS", filtered[X_AXIS], filtered[Y_AXIS], filtered[Z_AXIS],
                                       type=ClickablePlot.Type.SPLIT)
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
    column_names = [NAME, TIMESTAMP, X_AXIS, Y_AXIS, Z_AXIS]
    data = pd.read_csv(file_path, header=None, names=column_names, comment=';')
    data.dropna(axis=0, how='any', inplace=True)
    return filter_and_estimate(data)


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


##############################################################################
length, activities = read_data_and_filter(input_file())

if show_image():
    plot_images_and_exit(length, activities)

if show_plot():
    for activity, filtered, estimation in activities:
        plot_activity(activity + "--" + str(estimation) + "--" + str(filtered[TIMESTAMP].values[0]),
                      filtered[X_AXIS], filtered[Y_AXIS], filtered[Z_AXIS], estimation)
    plt.waitforbuttonpress()
    exit(0)

NEURAL_NETWORK(is_retraining(), length, activities).train()
