package com.capstone.carecabs.Utility

import android.os.Parcel
import android.os.Parcelable
import com.mapbox.geojson.Point

class ParcelablePoint(val point: Point) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readSerializable() as Point)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(point)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelablePoint> {
        override fun createFromParcel(parcel: Parcel): ParcelablePoint {
            return ParcelablePoint(parcel)
        }

        override fun newArray(size: Int): Array<ParcelablePoint?> {
            return arrayOfNulls(size)
        }
    }
}