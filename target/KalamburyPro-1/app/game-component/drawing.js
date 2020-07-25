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
const url = buildApiUrl(Util.API.WS, Util.IP.LOCAL, 8080, Util.APP_NAME, Util.RES.DRAW);
const drawingWebSocket = new WebSocket(url);

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
    window.location.href = Util.ROUTE.LOGIN;
}


// **** WEBSOCKET **** 
drawingWebSocket.onopen = function (event) {
    // first message is supposed to contain a token
    console.log('token sent: ', window.localStorage.getItem(Util.TOKEN_HEADER));
    drawingWebSocket.send(window.localStorage.getItem(Util.TOKEN_HEADER));
};
drawingWebSocket.onmessage = function (event) {
    console.log('Message received from teh server')
    readWebsocketMessage(JSON.parse(event.data));
};
drawingWebSocket.onclose = function (event) {
    if (event.wasClean) {
        console.log(`[close] Connection closed cleanly, code=${event.code} reason=${event.reason}`);
    } else {
        // e.g. server process killed or network down
        // event.code is usually 1006 in this case
        console.log('[close] Connection died');
    }
    redirectBackToLoginPage();
};
drawingWebSocket.onerror = function (event) {
    console.log('WebSocket error observed:', event);
    redirectBackToLoginPage();
};

// **** DRAWING **** 
canvas.onmousedown = function (event) {
    lastEvent = event;
    drawing = true;
};
canvas.onmouseup = function (event) {
    drawing = false;
};
canvas.onmousemove = function (event) {
    if (drawing === true) {
        const from = new Cartesian(lastEvent.offsetX, lastEvent.offsetY);
        const to = new Cartesian(event.offsetX, event.offsetY);
        const size = new Cartesian(canvas.width, canvas.height);

        context.beginPath();
        context.moveTo(from.x, from.y);
        context.lineTo(to.x, to.y);
        context.stroke();

        imageData = context.getImageData(0, 0, canvas.width, canvas.height);

        sendStroke(from, to, size);

        lastEvent = event;
    }
};

canvas.onmouseleave = function (event) {
    drawing = false;
};

// **** EVENTS HANDLING ****

function onDraw(drawingMessage) {
    const d = drawingMessage;
    if (d == null || d.from == null || d.to == null || d.size == null) {
        console.log('Wrong message!', d);
        return;
    }

    const scale = new Cartesian(canvas.width / d.size.x, canvas.height / d.size.y)
    const scaledFrom = new Cartesian(d.from.x * scale.x, d.from.y * scale.y);
    const scaledTo = new Cartesian(d.to.x * scale.x, d.to.y * scale.y);

    context.beginPath();
    context.moveTo(scaledFrom.x, scaledFrom.y);
    context.lineTo(scaledTo.x, scaledTo.y);

    context.stroke();

    imageData = context.getImageData(0, 0, canvas.width, canvas.height);
}

// **** SERVER COMMUNICATION ****

function readWebsocketMessage(msg) {
    if (msg == null) {
        console.error('[readWebsocketMessage] recieved invalid websocket message');
        return;
    }
    onDraw(msg);
}
/**
 * @param {Cartesian} from 
 * @param {Cartesian} to 
 * @param {Cartesian} size 
 */
function sendStroke(from, to, size) {
    const drawingMessage = new DrawingMessage(from, to, size);
    prepareWebsocketMessage(drawingMessage);
}

function clearCanvas() {

}

function prepareWebsocketMessage(drawingMessage) {
    if (drawingWebSocket.readyState === drawingWebSocket.OPEN) {
        drawingWebSocket.send(JSON.stringify(drawingMessage));
    }
}