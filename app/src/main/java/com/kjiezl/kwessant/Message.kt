package com.kjiezl.kwessant

class Message {
    var message: String? = null
    var senderId: String? = null
    var status: String? = null

    constructor(){}

    constructor(message: String?, senderId: String?, status: String?){
        this.message = message
        this.senderId = senderId
        this.status = status
    }
}