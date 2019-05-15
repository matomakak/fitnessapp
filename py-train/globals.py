#  Copyright (c) 2019. Martin Hlavačka

import argparse

import numpy as np

EXPORT_GRAPH = 'graph'

NAME = 'activity'  # identifying column name for exercise name in CSV input file
TIMESTAMP = 'timestamp'  # identifying column name for timestamp in CSV input file
X_AXIS = 'x-axis'  # identifying column name for x-axis values from sensor in CSV input file
Y_AXIS = 'y-axis'  # identifying column name for y-axis values from sensor in CSV input file
Z_AXIS = 'z-axis'  # identifying column name for z-axis values from sensor in CSV input file

INPUT_NAME = "I"  # identifying name of input data tensor within model
LABEL_NAME = "Y"  # identifying name of input label tensor within model
C_NAME = "c"  # identifying name of c tensor within model
LOSS_NAME = "Neg"  # identifying name of loss function tensor within model
ACCURACY_NAME = "accuracy"  # identifying name of accuracy tensor within model
OUTPUT_NAME = "O"  # identifying name of output tensor within model
RETRAIN_MODEL_SUFFIX = ':0'  # identifying name of suffix for tensors used to retrain within model

NUM_CHANNELS = 3  # channels within data used to train (X,Y,Z axes)
TRAINING_EPOCHS = 4  # number of epoch used in training
BATCH_SIZE = 10  # batch size used in data preparation part for training
LEARNING_RATE = 0.0001  # learning rate within training cnn

parser = argparse.ArgumentParser(
    description="Process input CSV file of recorded activities and trains CNN or visualize parsed data.")
parser.add_argument('-i', '--input', dest="input", help="CSV formatted file", metavar="FILE", required=True)
parser.add_argument('-o', '--output', dest="output", help="Trained neuron network prefix name", metavar="FILE")
parser.add_argument('-f', '--filter-input', dest="filter_input",
                    help="Information about repetitions in dataset", metavar="FILE")
parser.add_argument('--export-filter', dest="export_filter", help="Exports data from manual filtering and separating.",
                    action="store_true", default=False)
parser.add_argument('-m', '--model', dest="model", help="Previously trained neuron network prefix name", metavar="FILE")
parser.add_argument('--length', dest="sample_length",
                    help="Length of single sample, allowed values : \'max\',\'avg\' or ANY NUMBER", metavar='LENGTH')
parser.add_argument('-v', '--visualize', dest="visualize", help="Show plots of each recorded exercise",
                    action="store_true", default=False)
parser.add_argument('--image', dest='image', help="Show image of each recorded exercise chunk", action="store_true",
                    default=False)
parser.add_argument('--fourier', dest="fourier", help="Show plots of each DFT used for period estimation",
                    action="store_true", default=False)
parser.add_argument('-g', '--graph', dest="graph", help="Exports graph designed for TensorBoard",
                    action="store_true", default=False)
parser.add_argument('--estimations', dest="estimations", help="Show estimations(num. of records) of each activity",
                    action="store_true", default=False)
parser.add_argument('--auto-filter', dest="auto_filter",
                    help="Filter start and end of the recording and estimate period automatically", action="store_true",
                    default=False)


def convolve1d(signal):
    """Applies simple convolution(window size 20, each 20/20) to array of values"""
    ir = np.ones(20) / 20

    output = np.zeros_like(signal)

    for i in range(len(signal)):
        for j in range(len(ir)):
            if i - j < 0:
                continue
            output[i] += signal[i - j] * ir[j]

    return output


def initialize_and_validate_globals():
    """Parses and validates all argument options + custom dependency between them"""
    global args
    args = parser.parse_args()
    if args.visualize is False and args.output is None and args.image is False:
        parser.error("-o is required when -v is not set!")
    if args.model is not None and args.output is None:
        parser.error("-o is required when -m is not set!")
    if args.fourier is True and args.visualize is False:
        parser.error("-v is required when -f is set!")
    if args.sample_length is not None and args.output is None:
        parser.error("-o is required when --length is set!")
    if args.export_filter is True and args.filter_input is not None:
        parser.error("filter exporting cannot be used when -f is applied!")
    if args.filter_input is not None and args.auto_filter is True:
        parser.error("auto filter functionality turned on, filter input cannot be specified!")
    if args.sample_length is not None and args.sample_length != 'max' and args.sample_length != 'avg':
        try:
            int(args.sample_length)
        except ValueError:
            parser.error("allowed values for --length: \'max\',\'avg\' or ANY NUMBER!")


def sample_length():
    """Returns argument identifying length of single sample used to resize"""
    global args
    return args.sample_length


def output_name():
    """Returns argument identifying output file name without extension"""
    global args
    return args.output


def export_filter():
    """Returns argument identifying if manual filter process should be recorded and exported"""
    global args
    return args.export_filter


def filter_input():
    """Returns argument identifying manual filter export file name to use"""
    global args
    return args.filter_input


def is_auto_filter():
    """Returns argument identifying if automatic filter should be used"""
    global args
    return args.auto_filter


def is_retraining():
    """Returns argument identifying if already trained model was provided"""
    global args
    return args.model is not None


def input_file():
    """Returns argument identifying input CSV file"""
    global args
    return args.input


def show_estimation():
    """Returns argument identifying if estimations of sample size should be printed"""
    global args
    return args.estimations


def show_fourier():
    """Returns argument identifying if fourier based estimation graphs should be shown"""
    global args
    return args.fourier


def show_image():
    """Returns argument identifying if image representation of samples should be shown"""
    global args
    return args.image


def show_plot():
    """Returns argument identifying if graph representation of samples should be shown"""
    global args
    return args.visualize


def export_graph():
    """Returns argument identifying if tensorflow graph file should be created"""
    global args
    return args.graph
