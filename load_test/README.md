 # Keywords (in regex):

	* "(?:courseOffer=)(.*?)(?:\" class=\"ui-link ui-widget\">Beispielaufgaben</a>)" should return the courseOfferId after the Login
	* "(?:courseRecord=)(.*?)(?:\">)" should return the courseRecord after starting the Course; Important: Value is reused in same executionblock
	* "<h3>Einfache Mengenlehre" should exists after entering this Exercise

# See also

* [JACK3 Load Test Project](https://s3gitlab.paluno.uni-due.de/JACK/jack3-load-tests)
