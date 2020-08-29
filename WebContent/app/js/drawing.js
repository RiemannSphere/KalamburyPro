/**
 * @author Piotr Kołodziejski
 */

// **** CANVAS INIT ****
var canvas = document.getElementById('canvas');
var context = canvas.getContext('2d');
var imageData = context.getImageData(0, 0, canvas.width, canvas.height);
var canvasContainer = document.getElementById('canvas-container');
// resize canvas so it fits inside the column
canvas.width = canvasContainer.offsetWidth - 20;
canvas.height = canvas.width * 0.6;

// **** DRAWING INIT ****
var lastEvent;
var drawing = false;

// **** WEBSOCKET INIT ****
const urlDraw = buildApiUrl(Util.API.WS, Util.IP.LOCAL, 8080, Util.APP_NAME, Util.RES.DRAW);
const drawingWebSocket = new WebSocket(urlDraw);

// **** WINDOW ****
window.onresize = async function () {
	canvas.width = canvasContainer.offsetWidth - 20;
	canvas.height = canvas.width * 0.6;
	context = canvas.getContext('2d');
	var resizedImage = await resizeImageData(imageData, canvas.width, canvas.height);
	context.putImageData(resizedImage, 0, 0);
};

async function resizeImageData(imageData, width, height) {
	const resizeWidth = width >> 0;
	const resizeHeight = height >> 0;
	const ibm = await window.createImageBitmap(imageData, 0, 0, imageData.width, imageData.height, {
		resizeWidth, resizeHeight
	});
	const canvas = document.createElement('canvas');
	canvas.width = resizeWidth;
	canvas.height = resizeHeight;
	const ctx = canvas.getContext('2d');
	ctx.drawImage(ibm, 0, 0);
	return ctx.getImageData(0, 0, resizeWidth, resizeHeight);
};

function redirectBackToLoginPage() {
	drawingWebSocket.close();
	chatWebSocket.close();

	window.location.href = Util.ROUTE.Game2Login;
	window.localStorage.removeItem(Util.TOKEN_HEADER);
}


// **** WEBSOCKET **** 
drawingWebSocket.onopen = function (event) {
	// first message is supposed to contain a token
	console.log('DrawingWebSocket: token sent: ', window.localStorage.getItem(Util.TOKEN_HEADER));
	drawingWebSocket.send(window.localStorage.getItem(Util.TOKEN_HEADER));
};
drawingWebSocket.onmessage = function (event) {
	console.log('DrawingWebSocket: Message received from the server');
	readDrawWebsocketMessage(JSON.parse(event.data));
};
drawingWebSocket.onclose = function (event) {
	console.log(`DrawingWebSocket: Connection closed, code=${event.code} reason=${event.reason}`);
	redirectBackToLoginPage();
};
drawingWebSocket.onerror = function (event) {
	console.log('DrawingWebSocket: WebSocket error observed:', event);
	redirectBackToLoginPage();
};

// **** DRAWING **** 

var colorBlock = document.getElementById('color-block');
var ctx1 = colorBlock.getContext('2d');
var width1 = colorBlock.width;
var height1 = colorBlock.height;

var colorStrip = document.getElementById('color-strip');
var ctx2 = colorStrip.getContext('2d');
var width2 = colorStrip.width;
var height2 = colorStrip.height;

var colorLabel = document.getElementById('color-label');

var x = 0;
var y = 0;
var drag = false;
var rgbaColor = 'rgba(0,0,0,1)';

ctx1.rect(0, 0, width1, height1);
fillGradient();

ctx2.rect(0, 0, width2, height2);
var grd1 = ctx2.createLinearGradient(0, 0, 0, height1);
grd1.addColorStop(0, 'rgba(255, 0, 0, 1)');
grd1.addColorStop(0.085, 'rgba(255, 120, 0, 1)');
grd1.addColorStop(0.17, 'rgba(255, 255, 0, 1)');
grd1.addColorStop(0.255, 'rgba(120, 255, 0, 1)');
grd1.addColorStop(0.34, 'rgba(0, 255, 0, 1)');
grd1.addColorStop(0.425, 'rgba(0, 255, 120, 1)');
grd1.addColorStop(0.51, 'rgba(0, 255, 255, 1)');
grd1.addColorStop(0.595, 'rgba(0, 120, 255, 1)');
grd1.addColorStop(0.68, 'rgba(0, 0, 255, 1)');
grd1.addColorStop(0.765, 'rgba(120, 0, 255, 1)');
grd1.addColorStop(0.85, 'rgba(255, 0, 255, 1)');
grd1.addColorStop(0.935, 'rgba(255, 0, 120, 1)');
grd1.addColorStop(1, 'rgba(255, 0, 0, 1)');
ctx2.fillStyle = grd1;
ctx2.fill();

function fillGradient() {
	ctx1.fillStyle = rgbaColor;
	ctx1.fillRect(0, 0, width1, height1);

	var grdWhite = ctx2.createLinearGradient(0, 0, width1, 0);
	grdWhite.addColorStop(0, 'rgba(255,255,255,1)');
	grdWhite.addColorStop(1, 'rgba(255,255,255,0)');
	ctx1.fillStyle = grdWhite;
	ctx1.fillRect(0, 0, width1, height1);

	var grdBlack = ctx2.createLinearGradient(0, 0, 0, height1);
	grdBlack.addColorStop(0, 'rgba(0,0,0,0)');
	grdBlack.addColorStop(1, 'rgba(0,0,0,1)');
	ctx1.fillStyle = grdBlack;
	ctx1.fillRect(0, 0, width1, height1);
}

function changeColor(e) {
	x = e.offsetX;
	y = e.offsetY;
	var imageData = ctx1.getImageData(x, y, 1, 1).data;
	rgbaColor = 'rgba(' + imageData[0] + ',' + imageData[1] + ',' + imageData[2] + ',1)';
	colorLabel.style.backgroundColor = rgbaColor;
}

// Color Strip Events

colorStrip.onclick = function (e) {
	console.log('click');
	x = e.offsetX;
	y = e.offsetY;
	var imageData = ctx2.getImageData(x, y, 1, 1).data;
	rgbaColor = 'rgba(' + imageData[0] + ',' + imageData[1] + ',' + imageData[2] + ',1)';
	colorLabel.style.backgroundColor = rgbaColor;
	fillGradient();
};

// Color Block Events

colorBlock.onmousedown = function (e) {
	console.log('mousedown');
	drag = true;
	changeColor(e);
};
colorBlock.onmouseup = function (e) {
	console.log('mouseup');
	drag = false;
};
colorBlock.onmousemove = function (e) {
	console.log('mousemove');
	if (drag) {
		changeColor(e);
	}
};

// Main Canvas Events

canvas.onmousedown = function (event) {
	lastEvent = event;
	drawing = true;
};
canvas.onmouseup = function (event) {
	drawing = false;
};
canvas.onmousemove = function (event) {
	if (drawing === true && UserInfo.IS_DRAWING === true) {

		const from = new Cartesian(lastEvent.offsetX, lastEvent.offsetY);
		const to = new Cartesian(event.offsetX, event.offsetY);
		const size = new Cartesian(canvas.width, canvas.height);
		context.beginPath();
		context.strokeStyle = rgbaColor;
		context.moveTo(from.x, from.y);
		context.lineTo(to.x, to.y);
		context.stroke();

		imageData = context.getImageData(0, 0, canvas.width, canvas.height);

		sendStroke(from, to, size, rgbaColor);

		lastEvent = event;
	}
};

canvas.onmouseleave = function (event) {
	drawing = false;
};

// **** EVENTS HANDLING ****

/**
 * @param {DrawingMessage} drawingMessage 
 */

function onDraw(drawingMessage) {
	const d = drawingMessage;
	if (d == null || d.from == null || d.to == null || d.size == null || d.color == null) {
		console.log('DrawingWebSocket: Wrong message!', d);
		return;
	}

	const scale = new Cartesian(canvas.width / d.size.x, canvas.height / d.size.y);
	const scaledFrom = new Cartesian(d.from.x * scale.x, d.from.y * scale.y);
	const scaledTo = new Cartesian(d.to.x * scale.x, d.to.y * scale.y);

	context.beginPath();
	context.strokeStyle = d.color;
	context.moveTo(scaledFrom.x, scaledFrom.y);
	context.lineTo(scaledTo.x, scaledTo.y);

	context.stroke();

	imageData = context.getImageData(0, 0, canvas.width, canvas.height);
}

// **** SERVER COMMUNICATION ****

/**
 * @param {DrawingMessage} drawingMessage 
 */
function readDrawWebsocketMessage(msg) {
	if (msg == null) {
		console.error('DrawingWebSocket: [readDrawWebsocketMessage] recieved invalid websocket message');
		return;
	}
	onDraw(msg);
}
/**
 * @param {Cartesian} from 
 * @param {Cartesian} to 
 * @param {Cartesian} size 
 * @param {String} color 
 */
function sendStroke(from, to, size, color) {
	const drawingMessage = new DrawingMessage(from, to, size, color);
	prepareWebsocketMessage(drawingMessage);
}

/**
 * @param {DrawingMessage} drawingMessage 
 */
function prepareWebsocketMessage(drawingMessage) {
	if (drawingWebSocket.readyState === drawingWebSocket.OPEN) {
		drawingWebSocket.send(JSON.stringify(drawingMessage));
	}
}