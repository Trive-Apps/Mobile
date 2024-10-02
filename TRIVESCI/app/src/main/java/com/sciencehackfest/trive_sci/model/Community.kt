package com.sciencehackfest.trive_sci.model

import com.google.firebase.Timestamp

data class Community(
    var id: String = "",
    var fullname: String = "",
    var photoStatusUrl: String = "",
    var status: String = "",
    var totalLike: Int = 0,
    var totalComment: Int = 0,
    var totalBookmark: Int = 0,
    var createdAt: Timestamp? = null
)
