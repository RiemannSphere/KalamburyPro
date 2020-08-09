class Cartesian {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }
}
class DrawingMessage {
    constructor(from, to, size) {
        this.from = from;
        this.to = to;
        this.size = size;
    }
}

class ChatMessage {
    constructor(msgType, msgContent) {
        this.msgType = msgType;
        this.msgContent = msgContent;
    }
}

const MsgType = {
    WORD_TO_GUESS: 'WORD_TO_GUESS',
    MESSAGE: 'MESSAGE',
    YOU_GUESSED_IT: 'YOU_GUESSED_IT',
    NEXT_WORD: 'NEXT_WORD', // will also clean canvas
    CLEAN_CANVAS: 'CLEAN_CANVAS'
}
class Credentials{
    constructor(username, password) {
        this.username = username;
        this.password = password;
    }
}

