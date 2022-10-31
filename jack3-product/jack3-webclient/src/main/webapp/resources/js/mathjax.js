"use strict";

window.MathJax = {
  tex: {
    inlineMath: [['$', '$']]
  },

  svg: {
    fontCache: 'global'
  },   

  startup: {
    ready: () => {
      MathJax.startup.defaultReady();
      MathJax.startup.promise.then(() => {
        const observerConfig = {
          subtree: true,
          childList: true,
          characterData: true
        };

        function handleMutations(mutations, observer) {
          let nodesToTypeset = [];

          for (let mutation of mutations) {
            switch (mutation.type) {

              // One or several child nodes were added or removed.
              // We clear cached information for removed nodes and book added ones for typesetting.
              case 'childList':
                MathJax.typesetClear(Array.from(mutation.removedNodes));
                nodesToTypeset.push(...Array.from(mutation.addedNodes));
                break;

              // The text of a node changed. We have to clear the cache and typeset that node.
              case 'characterData':
                MathJax.typesetClear(mutation.target);
                nodesToTypeset.push(mutation.target);
                break;

              // We issue a warning when we have an unknown event type.
              default:
                console.warn('Unexpected mutation type:' + mutation.type);
            }
          }

          // We re-typeset all connected nodes with the observer disconnected
          // because the rendering process can itself cause DOM changes.
          if (nodesToTypeset.length) {
            observer.disconnect();
            MathJax.typesetPromise(nodesToTypeset.filter(n => n.isConnected))
             .finally(() => observer.observe(document.body,observerConfig));
          }
        }

        let observer = new MutationObserver(handleMutations);
        observer.observe(document.body,observerConfig);  
      });
    }
  }
};