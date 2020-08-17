const wordToGuessTextArea = document.getElementById('wordToGuess');
const messagesTextArea = document.getElementById('messagesTextArea');
const messageTextInput = document.getElementById('messageText');

// **** WEBSOCKET INIT ****
const urlChat = buildApiUrl(Util.API.WS, Util.IP.LOCAL, 8080, Util.APP_NAME, Util.RES.CHAT);
const chatWebSocket = new WebSocket(urlChat);

// **** WEBSOCKET **** 
chatWebSocket.onopen = function (event) {
    // first message is supposed to contain a token
    console.log('ChatWebSocket: token sent: ', window.localStorage.getItem(Util.TOKEN_HEADER));
    chatWebSocket.send(window.localStorage.getItem(Util.TOKEN_HEADER));
};
chatWebSocket.onmessage = function (event) {
    console.log('ChatWebSocket: Message received from the server');
    console.log(event);
    console.log(event.data);
    readChatWebsocketMessage(JSON.parse(event.data));
};
chatWebSocket.onclose = function (event) {
    console.log(`ChatWebSocket: Connection closed, code=${event.code} reason=${event.reason}`);
    redirectBackToLoginPage();
};
chatWebSocket.onerror = function (event) {
    console.log('ChatWebSocket: WebSocket error observed:', event);
    redirectBackToLoginPage();
};

// **** EVENTS ****
messageTextInput.onkeyup = function(event) {
    if(event.keyCode === 13) {
        sendMessage();
    }
}

// **** SERVER COMMUNICATION ****

/**
 * @param {ChatMessage} chatMessage 
 */
function readChatWebsocketMessage(msg) {
    if (msg == null) {
        console.error('ChatWebSocket: [readChatWebsocketMessage] recieved invalid websocket message');
        return;
    }
    onChat(msg);
}

function cleanCanvasAndGenerateNewWord() {
    console.log('ChatWebSocket: cleanCanvasAndGenerateNewWord');
}

function sendMessage() {
    const msgContent = messageTextInput.value;
    const msgType = MsgType.MESSAGE;
    const msg = new ChatMessage(msgType, msgContent);
    chatWebSocket.send(JSON.stringify(msg));
    messageTextInput.value = "";
    console.log('ChatWebSocket: Message sent: ', msg);
}

function cleanCanvas() {
    const msgContent = "";
    const msgType = MsgType.CLEAN_CANVAS;
    const msg = new ChatMessage(msgType, msgContent);
    chatWebSocket.send(JSON.stringify(msg));
    console.log('ChatWebSocket: Message sent: ', msg);
}

// **** EVENTS HANDLING ****

/**
 * @param {ChatMessage} chatMessage 
 */
function onChat(chatMessage) {
	const d = chatMessage;
	if (d == null || d.msgType == null || d.msgContent == null || !Object.values(MsgType).includes(d.msgType) ) {
		console.log('ChatWebSocket: Wrong message!', d);
		return;
	}

    if(d.msgType === MsgType.WORD_TO_GUESS) {
        UserInfo.IS_DRAWING = true;
        onNewWordToGuess(d.msgContent);
        return;
    }
    
    if(d.msgType === MsgType.MESSAGE) {
        onMessage(d.msgContent);
        return;
    }

    if(d.msgType === MsgType.YOU_GUESSED_IT) {
        onWordGuessSuccess();
        return;
    }

    if(d.msgType === MsgType.CLEAN_CANVAS) {
        onCleanCanvas();
        return;
    }

    if(d.msgType === MsgType.CLEAN_WORD_TO_GUESS) {
        UserInfo.IS_DRAWING = false;
        onNewWordToGuess("");
        return;
    }
}

/**
 * @param {string} word 
 */
function onNewWordToGuess(word) {
    wordToGuessTextArea.value = word;
}

/**
 * @param {string} msg 
 */
function onMessage(msg) {
    messagesTextArea.value += msg + '\n';
}

function onWordGuessSuccess() {
    onMessage('Zgadłeś!');
    cleanCanvasAndGenerateNewWord();
}

function onCleanCanvas() {
    context.clearRect(0, 0, canvas.width, canvas.height);
    console.log('Cleaning canvas...');
}