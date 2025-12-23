package com.znam.app

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val questionText: String,
    val options: String,
    val correctAnswer: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(questionText)
        parcel.writeString(options)
        parcel.writeString(correctAnswer)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getParsedOptions(): List<String> {
        return options.split(";").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    companion object CREATOR : Parcelable.Creator<Question> {
        override fun createFromParcel(parcel: Parcel): Question {
            return Question(parcel)
        }

        override fun newArray(size: Int): Array<Question?> {
            return arrayOfNulls(size)
        }
    }
}
