from enum import Enum

import matplotlib.lines as lines
import matplotlib.pyplot as plt


class ClickablePlot:
    """
    Class providing functionality for interactive graph allowing to identify specific features of data
    """
    class Type(Enum):
        """
        Enum providing type of graph
        """
        FILTER = 1  # used for in and out filtering
        SPLIT = 2  # used for repetition separation within one dataset

    def __init__(self, fig, axs, plot_type):
        self.fig = fig
        self.axs = axs
        self.aspan_list_out = []
        self.aspan_list_in = []
        self.type = plot_type
        self.x_max = axs[0].get_xlim()[1]
        self.min = 0
        self.max = 0
        self.filtered_in = False
        self.filtered_out = False
        self.finished = False
        self.split_lines = []
        self.consumed = False

        self.fig.canvas.mpl_connect('button_press_event', self.onclick)
        self.fig.canvas.mpl_connect('pick_event', self.line_onclick)

    def onclick(self, event):
        """
        Parses click event within graph and according to type delegates request for processing
        :param event: click event
        """
        if self.consumed:
            self.consumed = False
        elif event.button == 3:
            if (self.filtered_in and self.filtered_out) or len(self.split_lines) > 1:
                self.finished = True
        elif event.xdata is None:
            return
        elif self.type == ClickablePlot.Type.FILTER:
            self.filter_click(event)
        elif self.type == ClickablePlot.Type.SPLIT:
            self.split_click(event)

    def filter_click(self, event):
        """
        Filter type graph click funtionality showing in and out parts greyed out which are identified
        :param event: click event
        """
        if event.xdata > (self.x_max / 2):
            for aspan in self.aspan_list_out:
                aspan.remove()
            self.aspan_list_out = list()
            self.max = event.xdata
            self.filtered_out = True
            for ax in self.axs:
                self.aspan_list_out.append(ax.axvspan(self.max, self.x_max, facecolor='0.9', alpha=0.5))
        else:
            for aspan in self.aspan_list_in:
                aspan.remove()
            self.aspan_list_in = list()
            self.min = event.xdata
            self.filtered_in = True
            for ax in self.axs:
                self.aspan_list_in.append(ax.axvspan(0, self.min, facecolor='0.9', alpha=0.5))

    def split_click(self, event):
        """
        Exercise repetition identification type graph click functionality allowing to specify parts of graph which
        should be separated
        :param event: click event
        """
        x = [event.xdata, event.xdata]
        y = [-100, 100]
        added_lines = []
        for ax in self.axs:
            added_lines.append(ax.add_line(lines.Line2D(x, y, picker=5, color='red')))
            ax.get_figure().canvas.draw_idle()
        self.split_lines.append(added_lines)

    def line_onclick(self, event):
        """
        Functionality allowing to manipulate with already created lines within SPLIT type graph
        :param event: click event
        """
        if event.mouseevent.button == 3:
            for lines in self.split_lines:
                if event.artist in lines:
                    for line in lines:
                        line.remove()
                    self.split_lines.remove(lines)
                    break
            self.consumed = True

    def wait_for_finish(self):
        """
        Functionality allowing to close graph under specific circumstances according to graph type
        :return: case of:
            FILTER - array containing in and out points
            SPLIT - array of all separations made within graph
        """
        while not self.finished:
            plt.waitforbuttonpress()
        plt.close(self.fig)
        if self.type == ClickablePlot.Type.FILTER:
            return [int(self.min), int(self.max) + 1]
        elif self.type == ClickablePlot.Type.SPLIT:
            return sorted(
                [int(lines[0].get_xdata()[0]) for lines in self.split_lines if lines[0].get_xdata()[0] is not None])
