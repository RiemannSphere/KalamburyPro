/**
 * @author Maciej Szabała
 */

const wordToGuessTextArea = document.getElementById('wordToGuess');
const messagesTextArea = document.getElementById('messagesTextArea');
const messageTextInput = document.getElementById('messageText');
const cleanCanvasBtn = document.getElementById('cleanCanvasBtn');
const scoreboard = document.getElementById('scoreboard');

UserInfo.IS_DRAWING = false;
onIsDrawingChange(UserInfo.IS_DRAWING);

// Active Users List
var activeUsers;
// var activeUsers = [
//     new Score('User1', false, 10),
//     new Score('User2', false, 0),
//     new Score('User3', true, 1),
//     new Score('User4', false, 5),
// ];

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
messageTextInput.onkeyup = function (event) {
    if (event.keyCode === 13) {
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
    if (UserInfo.IS_DRAWING === true) {
        const msgContent = "";
        const msgType = MsgType.CLEAN_CANVAS;
        const msg = new ChatMessage(msgType, msgContent);
        chatWebSocket.send(JSON.stringify(msg));
        console.log('ChatWebSocket: Message sent: ', msg);
    }
}

// **** EVENTS HANDLING ****

/**
 * @param {ChatMessage} chatMessage 
 */
function onChat(chatMessage) {
    const d = chatMessage;
    if (d == null || d.msgType == null || d.msgContent == null || !Object.values(MsgType).includes(d.msgType)) {
        console.log('ChatWebSocket: Wrong message!', d);
        return;
    }

    if (d.msgType === MsgType.WORD_TO_GUESS) {
        // make some elements visible
        UserInfo.IS_DRAWING = true;
        onIsDrawingChange(UserInfo.IS_DRAWING);

        onNewWordToGuess(d.msgContent);
        return;
    }

    if (d.msgType === MsgType.MESSAGE) {
        onMessage(d.msgContent);
        return;
    }

    if (d.msgType === MsgType.YOU_GUESSED_IT) {
        onWordGuessSuccess();
        return;
    }

    if (d.msgType === MsgType.CLEAN_CANVAS) {
        onCleanCanvas();
        return;
    }

    if (d.msgType === MsgType.CLEAN_WORD_TO_GUESS) {
        // make some elements invisible
        UserInfo.IS_DRAWING = false;
        onIsDrawingChange(UserInfo.IS_DRAWING);

        onNewWordToGuess("");
        return;
    }

    if (d.msgType === MsgType.SCOREBOARD) {
        onScoreboard(d.msgContent);
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
    messagesTextArea.scrollTop = messagesTextArea.scrollHeight;
}

function onWordGuessSuccess() {
    onMessage('Zgadłeś!');
    cleanCanvasAndGenerateNewWord();
}

function onCleanCanvas() {
    context.clearRect(0, 0, canvas.width, canvas.height);
    imageData = context.getImageData(0, 0, canvas.width, canvas.height);
    console.log('Cleaning canvas...');
}

/**
 * Based on isDrawing value allow drawing, allow cleaning canvas etc.
 * @param {boolean} isDrawing 
 */
function onIsDrawingChange(isDrawing) {
    if (isDrawing === true) {
        // allow cleaning canvas
        cleanCanvasBtn.style.visibility = "visible";
    } else {
        // disallow cleaning canvas
        cleanCanvasBtn.style.visibility = "hidden";
    }
}

/**
 * Receives stringified list of active users from a backend
 * @param {string} activeUsersMsg 
 */
function onScoreboard(activeUsersMsg) {
    if(activeUsersMsg == null || activeUsersMsg.length === 0) {
        return;
    }
    console.log('Active users: ', activeUsers);
    activeUsers = JSON.parse(activeUsersMsg);
    if(scoreboard.style.visibility === 'visible') {
        // replace exisitng list
        scoreboard.replaceChild(usersArrayToHtmlList(activeUsers), document.getElementById('usersList'));
    }
}

/**
 * On click
 */
function onScoreboardDisplay() {
    // make scoreboard visible
    if(scoreboard.style.visibility === 'hidden') {
        scoreboard.style.visibility = 'visible';
        // replace exisitng list
        scoreboard.replaceChild(usersArrayToHtmlList(activeUsers), document.getElementById('usersList'));
    } else {
        scoreboard.style.visibility = 'hidden';
    }
}

/**
 * Produces HTML unordered list for given array of users
 * @param {Array<ActiveUser>} userArray 
 */
function usersArrayToHtmlList(userArray) {
    const list = document.createElement('ul');
    list.setAttribute('id', 'usersList')

    for (var i = 0; i < userArray.length; i++) {
        const item = document.createElement('li');
        if(userArray[i] == null) {
           continue; 
        }
        // @see https://emojipedia.org/
        const emoji = userArray[i].isDrawing ? '🖍️' : '🤷‍♂️';
        item.appendChild(document.createTextNode( emoji + ' ' + userArray[i].username + ': ' + userArray[i].points));
        list.appendChild(item);
    }
    return list;
}