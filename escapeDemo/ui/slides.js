
var slideNum = new Array();
var numSlides = new Array();
var numSlideShows = 0;

function isClass(object, className) {
	return (object.className.search('(^|\\s)' + className + '(\\s|$)') != -1);
}

function GetElementsWithClassName(elementName, className) {
	var allElements = document.getElementsByTagName(elementName);
	var elemColl = new Array();
	for (i = 0; i< allElements.length; i++) {
		if (isClass(allElements[i], className)) {
			elemColl[elemColl.length] = allElements[i];
		}
	}
	return elemColl;
}

function slideLabel() {
	while(true) {
		var slideShowIdentifier = 'slide' + numSlideShows;
		var slideColl = GetElementsWithClassName('div', slideShowIdentifier);
		var smax = slideColl.length;
		if(smax == 0) {
			return;
		}
		var captionIdentifier = 'caption' + numSlideShows;
		var captionColl = GetElementsWithClassName('div', captionIdentifier);
		numSlides[numSlideShows] = smax;
		for (n = 0; n < smax; n++) {
			var obj = slideColl[n];
			obj.setAttribute('id', 'slide' + numSlideShows + n);
			if(n < captionColl.length) {
				obj = captionColl[n];
				obj.setAttribute('id', 'caption' + numSlideShows + n);
			}
		}
		numSlideShows++;
	}
}

function displayCurrentSlide(slideShow, currentSlide, theNum) {
	var cs = document.getElementById('currentSlide' + slideShow);
	if(!cs) {
		return;
	}
	if(theNum < 2) {
		cs.style.visibility = 'hidden';
	}
	else {
		cs.innerHTML = (currentSlide + 1) + '<span> / ' + theNum + '</span>';
	}
}

function displayBackControl(slideShow, theSlide, theNum) {
	controlsDiv = document.getElementById('backControl' + slideShow);
	if (!controlsDiv) return;
	var htmlString = ' ';
	if(theNum < 2) {
		controlsDiv.style.visibility = 'hidden';
		return;
	}
	if(theNum > 2 || theSlide == 1) {
		var prevString = 'previous';
		if(theSlide == 0) {
			prevString = 'final';
		}
	
		var htmlString = 
			'<form action="#" id="backControlForm' 
			+ slideShow
			+ '">' +
			'<div>' +
			'<a accesskey="z" id="prev' +
			slideShow +
			'" href="javascript:go(' + slideShow + ', -1);">&laquo; ' 
			+ prevString + 
			'</a>' +
			'</div>' +
			'</form>';
	}
	else if(theSlide == 0) {
		htmlString = getForwardString(slideShow, theSlide, theNum);
	}
	controlsDiv.innerHTML = htmlString;
}

function getForwardString(slideShow, theSlide, theNum) {
	var nextString = 'next';
	if(theSlide == theNum - 1) {
		nextString = 'initial';
	}
	htmlString = 
		'<form action="#" id="forwardControlForm' 
		+ slideShow
		+ '">' +
		'<div>' +
		'<a accesskey="x" id="next' +
		slideShow +
		'" href="javascript:go(' + slideShow + ', 1);"> ' 
		+ nextString + 
		' &raquo;</a>' +
		'</div>' +
		'</form>';
	return htmlString;
}

function displayForwardControl(slideShow, theSlide, theNum) {
	controlsDiv = document.getElementById('forwardControl' + slideShow);
	if (!controlsDiv) return;
	var htmlString = ' ';
	if(theNum > 2) {
		htmlString = getForwardString(slideShow, theSlide, theNum);
		controlsDiv.innerHTML = htmlString;
	}
	else {
		controlsDiv.style.visibility = 'hidden';
	}
}


function displayControls(slideShow, theSlide, theNum) {
	displayForwardControl(slideShow, theSlide, theNum);
	displayBackControl(slideShow, theSlide, theNum);
}


function displaySlide(slideShow, theSlide, theNum) {
	if(theNum == 0) {
		return;
	}
	var ne = document.getElementById('slide' + slideShow + theSlide);
	ne.style.visibility = 'visible';
	ne = document.getElementById('caption' + slideShow + theSlide);
	if(ne) {
		ne.style.visibility = 'visible';
	}
	if(theNum > 1) {
		displayCurrentSlide(slideShow, theSlide, theNum);
		displayControls(slideShow, theSlide, theNum);
	}
}

function hideSlide(slideShow, theSlide) {
	var pe = document.getElementById('slide' + slideShow + theSlide);
	pe.style.visibility = 'hidden';
	pe = document.getElementById('caption' + slideShow + theSlide);
	if(pe) {
		pe.style.visibility = 'hidden';
	}
}

function go(slideShow, inc) {
	if (document.getElementById("slideProj").disabled) {
		return;
	}
	var num = numSlides[slideShow];
	if(num > 1) {
		var prevSlide = slideNum[slideShow];
		var theSlide = (prevSlide + inc + num) % num;
		var ne = document.getElementById('slide' + slideShow + theSlide);
		if (!ne) {
			theSlide = 0;
		}
		slideNum[slideShow] = theSlide;
		hideSlide(slideShow, prevSlide);
		displaySlide(slideShow, theSlide, num);
	}
}

function startup() {
	if (document.getElementById("slideProj").disabled) {
		return;
	}
	slideLabel();
	for(i = 0; i < numSlideShows; i++) {
		slideNum[i] = 0;
		var num = numSlides[i];
		for(j = 1; j < num; j++) {
			hideSlide(i, j);
		}
		displaySlide(i, 0, num);
	}
}

window.onload = startup;
