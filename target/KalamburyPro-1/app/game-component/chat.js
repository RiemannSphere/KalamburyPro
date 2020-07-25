var chatWebSocket = new WebSocket("ws://10.131.180.187:8080/KalamburyPro/chat");

var messagesTextArea = document.getElementById("messagesTextArea");
var randomText = document.getElementById("randomText");
var messageText = document.getElementById("messageText");

const WORD = "WORD";
const FIRST_MESSAGE = "FIRST_MESSAGE";
const NEXT = "next"

chatWebSocket.onmessage = function processMessage(message) {
	var jsonData = JSON.parse(message.data);
	var type = jsonData.type;
	var text = jsonData.txt;

	if (jsonData.message != null) {

		messagesTextArea.value += jsonData.message + "\n";

		messagesTextArea.scrollTop = messagesTextArea.scrollHeight;

	}
	if( type === WORD || type === FIRST_MESSAGE ){
		randomText.value = text;
	}

}

function nextText() {
	chatWebSocket.send(NEXT);
}

function sendMessage() {
	chatWebSocket.send(messageText.value);
	messageText.value = "";
}