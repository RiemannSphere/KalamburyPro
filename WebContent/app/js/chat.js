const wordToGuessTextArea = document.getElementById('wordToGuess');
const messagesTextArea = document.getElementById('messagesTextArea');

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
    console.error('ChatWebSocket: cleanCanvasAndGenerateNewWord has not been implemented yet');
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
        onNewWordToGuess(d.msgContent);
        return;
    }
    
    if(d.msgType === MsgType.MESSAGE) {
        onMessage(d.msgContent);
        return;
    }

    if(d.msgType === MsgType.YOU_GUESSED_IT) {
        onWordGuessSuccess(d.msgContent);
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

/**
 * @param {boolean} wordGuessed 
 */
function onWordGuessSuccess(wordGuessed) {
    if(wordGuessed) {
        cleanCanvasAndGenerateNewWord();
    }
}