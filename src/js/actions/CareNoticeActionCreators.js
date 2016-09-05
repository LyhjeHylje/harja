import Dispatcher from '../Dispatcher';
import Constants from '../Constants';

/* eslint-disable no-console */

export default {
  addItem(data) {
    Dispatcher.handleViewAction({
      type: Constants.ActionTypes.CARENOTICE_ADDED,
      data: data
    });
  },

  clearList() {
    console.warn('clearList action not yet implemented...');
  },

  completeTask(task) {
    console.warn('completeTask action not yet implemented...', task);
  }
};
