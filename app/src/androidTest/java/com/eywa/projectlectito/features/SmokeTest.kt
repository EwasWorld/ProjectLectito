package com.eywa.projectlectito.features

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eywa.projectlectito.features.utils.ActivityScenarioRobot
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @Test
    fun smokeTest() {
        ActivityScenarioRobot {
            val textName = "New Text"
//            clickAddTextButton {
//                inputTextName(textName)
//                clickOk()
//            }

//            clickAddPageMenuItem(textName) {
//                inputChapter("1")
//                inputPage("2")
//                inputContent("拝啓、父さん、母さん。 \nお元気でしようか。")
//                clickAdd()
//            }

            clickText(textName) {
                setSelectModeAutoColoured()
                clickParsedWord("母")
                setSelectModeAutoColoured()
            }
        }
    }
}