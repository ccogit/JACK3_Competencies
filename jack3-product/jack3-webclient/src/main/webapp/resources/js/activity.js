window.Jack || ( window.Jack = {} );

if (!Jack.activityBar) {

  const barQuery = '#activity-bar';

  Jack.activityBar = {

    start : function() {
      $(barQuery).addClass('active');
    },

    stop : function() {
      $(barQuery).removeClass('active');
    },

    setErrorState : function(errorState) {
	  let bar = $(barQuery);
      if (errorState) {
        bar.addClass('error');
      } else {
        bar.removeClass('error');
      }
    },

    handlePrimeFacesAjaxComplete: function(xhr,settings) {
      // We check if the response will cause a redirect.
      // In this case we want the animation to continue until the reload.
      if (settings.responseXML) {
        let partialResponse = settings.responseXML.getElementsByTagName('partial-response')[0];
        for (const node of partialResponse.childNodes) {
	      if (node.nodeName === 'redirect') {
            return;
          }
        }
      }

      // No redirect will happen, we stop the animation.
      Jack.activityBar.stop();
    }
  }

  $(document).on('pfAjaxStart',Jack.activityBar.start);
  $(document).on('pfAjaxError',() => Jack.activityBar.setErrorState(true));
  $(document).on('pfAjaxSuccess',() => Jack.activityBar.setErrorState(false));
  $(document).on('pfAjaxComplete',Jack.activityBar.handlePrimeFacesAjaxComplete);
}
