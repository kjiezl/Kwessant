package com.kjiezl.kwessant

class User {
    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var status: String? = null
    var latestMessage: String? = null
    var fcmToken: String? = null

    constructor(){}

    constructor(name: String?, email: String?, uid: String?, status: String?, latestMessage: String){
        this.name = name
        this.email = email
        this.uid = uid
        this.status = status
        this.latestMessage = latestMessage
    }
}