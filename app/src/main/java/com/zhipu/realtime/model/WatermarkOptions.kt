package com.zhipu.realtime.model

class WatermarkOptions {
    var visibleInPreview: Boolean = true
    var positionInLandscapeMode: Rectangle = Rectangle()
    var positionInPortraitMode: Rectangle = Rectangle()

    class Rectangle {
        var x: Int = 0
        var y: Int = 0
        var width: Int = 0
        var height: Int = 0

        constructor() {
            this.x = 0
            this.y = 0
            this.width = 0
            this.height = 0
        }

        constructor(x_: Int, y_: Int, width_: Int, height_: Int) {
            this.x = x_
            this.y = y_
            this.width = width_
            this.height = height_
        }
    }
}