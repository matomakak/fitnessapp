import argparse

EXPORT_GRAPH = 'graph'

NAME = 'activity'
TIMESTAMP = 'timestamp'
X_AXIS = 'x-axis'
Y_AXIS = 'y-axis'
Z_AXIS = 'z-axis'

INPUT_NAME = "I"
LABEL_NAME = "Y"
C_NAME = "c"
LOSS_NAME = "Neg"
ACCURACY_NAME = "accuracy"
OUTPUT_NAME = "O"
RETRAIN_MODEL_SUFFIX = ':0'

NUM_CHANNELS = 3
TRAINING_EPOCHS = 4
BATCH_SIZE = 10
LEARNING_RATE = 0.0001

parser = argparse.ArgumentParser(
    description="Process input CSV file of recorded activities and trains CNN or visualize parsed data.")
parser.add_argument('-i', '--input', dest="input", help="CSV formatted file", metavar="FILE", required=True)
parser.add_argument('-o', '--output', dest="output", help="Trained neuron network prefix name", metavar="FILE")
parser.add_argument('-m', '--model', dest="model", help="Previously trained neuron network prefix name", metavar="FILE")
parser.add_argument('--length', dest="sample_length",
                    help="Length of single sample, allowed values : \'max\',\'avg\' or ANY NUMBER", metavar='LENGTH')
parser.add_argument('-v', '--visualize', dest="visualize", help="Show plots of each recorded exercise",
                    action="store_true", default=False)
parser.add_argument('--image', dest='image', help="Show image of each recorded exercise chunk", action="store_true",
                    default=False)
parser.add_argument('-f', '--fourier', dest="fourier", help="Show plots of each DFT used for period estimation",
                    action="store_true", default=False)
parser.add_argument('-g', '--graph', dest="graph", help="Exports graph designed for TensorBoard",
                    action="store_true", default=False)
parser.add_argument('--estimations', dest="estimations", help="Show estimations(num. of records) of each activity",
                    action="store_true", default=False)
parser.add_argument('--auto-filter', dest="auto_filter",
                    help="Filter start and end of the recording and estimate period automatically", action="store_true",
                    default=False)


def initialize_and_validate_globals():
    global args
    args = parser.parse_args()
    if args.visualize is False and args.output is None and args.image is False:
        parser.error("-o is required when -v is not set.")
    if args.model is not None and args.output is None:
        parser.error("-o is required when -m is not set.")
    if args.fourier is True and args.visualize is False:
        parser.error("-v is required when -f is set.")
    if args.sample_length is not None and args.output is None:
        parser.error("-o is required when --length is set.")
    if args.sample_length is not None and args.sample_length != 'max' and args.sample_length != 'avg':
        try:
            int(args.sample_length)
        except ValueError:
            parser.error("allowed values for --length: \'max\',\'avg\' or ANY NUMBER")


def sample_length():
    global args
    return args.sample_length


def output_name():
    global args
    return args.output


def is_auto_filter():
    global args
    return args.auto_filter


def is_retraining():
    global args
    return args.model is not None


def input_file():
    global args
    return args.input


def show_estimation():
    global args
    return args.estimations


def show_fourier():
    global args
    return args.fourier


def show_image():
    global args
    return args.image


def show_plot():
    global args
    return args.visualize


def export_graph():
    global args
    return args.graph
