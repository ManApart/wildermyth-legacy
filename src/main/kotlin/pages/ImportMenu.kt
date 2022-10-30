package pages

import el
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.input
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

fun importMenu() {
    val section = el("import-section")
    section.innerHTML = ""
    document.getElementById("character-detail-section")!!.innerHTML = ""
    document.getElementById("nav")!!.innerHTML = ""
    section.append {
        h3 { +"View your own characters from Wildermyth!" }
        button {
            +"Cancel"
            onClickFunction = {
                section.innerHTML = ""
                buildNav()
            }
        }
        input(InputType.file) {
            id = "import-input"
            type = InputType.file
            onChangeFunction = {
                buildNav()
                val element = document.getElementById(id) as HTMLInputElement
                if (element.files != undefined) {
                    val file = element.files!![0]!!
                    val reader = FileReader()
                    reader.onload = {
                        importZip(reader.result as ArrayBuffer, window.location.hash)
                    }
                    reader.onerror = { error ->
                        console.error("Failed to read File $error")
                    }
                    reader.readAsArrayBuffer(file)
                }
            }
        }
        div {
            id = "upload-instructions"

            div {
                id = "instructions-text"
                h4 { +"Using the Uploader" }
                p { +"If this all seems a bit much, try using download tool the instead. If we're lucky, it will find your game paths and files and create the zip for you. You'll still need to export each character though if you want character pictures." }
                a("./wilder-upload.exe") {
                    id = "download-tool-link"
                    +"Download Tool"
                }

                h4 { +"Initial Setup" }
                p { +"To set everything up, we'll need to gather some files and create a single zip to upload. The completed zip should look like the picture to the right/below." }
                ol {
                    li { +"In game, go to My Legacy -> Heroes -> Hero List." }
                    li {
                        +"For each hero you want to view."
                        ul {
                            li { +"Click their picture to view their character sheet." }
                            li { +"Click 'Customize' (or press 7)" }
                            li { +"Click 'Export Character'" }
                            li { +"This should open a new window, which you can ignore until you've exported each character." }
                            li { +"Click the change hero arrow above their name to go to the next hero." }
                        }
                    }
                    li { +"Navigate to your '<Game Install>/out' folder." }
                    ul {
                        li { +"If you just exported characters, it should be one folder up from the character export." }
                        li { +"Otherwise it should be 'steamapps/common/Wildermyth/out' or the GOG equivalent." }
                    }
                    li { +"Select all the character folders and create a zip file." }
                    ul {
                        li { +"This gives us all the character pictures for the portraits." }
                        li { a("https://www.7-zip.org/", target = "_blank") { +"7Zip is a good tool for this." } }
                    }
                    li { +"Add your legacy.json file to the zip" }
                    ul {
                        li { +"This file gives us all the actual character data and character relationships." }
                        li { +"Navigate to your '<Game Install>/players/<your-player-name>' folder." }
                        li { +"Open or extract 'legacy.json.zip' and grab the 'legacy.json' file." }
                        li { +"Add legacy.json file to the characters zip. If you extracted the json file you can drag it onto the zip file." }
                    }
                    li { +"Upload the zip using the button below." }
                    li { +"While nothing is stored server side, the zip contents are stored in the browsers local storage. This means reloading the page won't drop your data!" }
                }

                h4 { +"Exporting All Characters" }
                p { +"If you have a lot of characters, you can use this autohotkey script to automate the character export." }
                ol {
                    li {
                        +"Download and install "
                        a("https://www.autohotkey.com/", target = "_blank") { +"AutoHotkey." }
                    }
                    li {
                        +"Download and open the "
                        a("./WildermythLegacy.ahk") { +"wildermyth script." }
                    }
                    li {
                        +"In the game, navigate to your heroes legacy, where you see all your characters standing around. The script has to start there. When it starts it will open you hero list, and then start clicking through each character."
                    }
                    li {
                        +"The Loop runs up to 700 times (so max 700 characters). If you don't manually stop it, it will keep running (because it doesn't know how many characters you have), and so it could re-export characters (which is fine)."
                    }
                    li {
                        +"To start (and stop) the script, press Ctrl + e."
                    }
                }

                h4 { +"Additional Info File" }
                ol {
                    li { +"Any editing of tags or character journals on the site is stored locally in your browser." }
                    li { +"Clicking the 'Export' button lets you download this json file so you can back it up." }
                    li { +"You can include this json file at the top level of the zip you upload to the site and it will be loaded." }
                    li { +"This let's you use a single zip to load your data onto multiple devices." }
                }
                h4 { +"Additional Properties Files" }
                ol {
                    li { +"In order to get the text for history entries, items, and abilities you need to provide the several properties files from the game." }
                    li { +"Go to <GameFolder>/Wildermyth/assets/text/" }
                    li { +"Grab aspects/aspects.properties" }
                    li { +"Grab dynamic/dynamic.properties" }
                    li { +"Grab story/story.properties" }
                    li { +"Add each file to the zip you upload." }
                    li { +"Now character bios (that you haven't overridden already) will no longer be blank and you'll get text for abilities and gear!" }
                }
            }
            img {
                id = "zip-example"
                src = "./example/instructions.png"
            }
        }

    }
}

