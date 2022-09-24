#h::Reload

#IfWinActive, Wildermyth
#MaxThreadsPerHotkey 3
^e:: 
#MaxThreadsPerHotkey 1
if keep_running
{
     keep_running := false  ; Signal the other thread to stop.
     return
}
keep_running := true

  Sleep 100
  Send {Enter}
  Sleep 1000

  ; Click First Hero
  Click_screen(800, 332)
  Sleep 100

  Send, 7
  Sleep 100

Loop 700 
{
  ;Export Character
  Click_Screen(912, 640)
  Sleep,200

  ;Click back in
  Click_Screen(918, 370)
  Sleep,100

  ;Next Hero
  Click_Screen(560, 110)
  Sleep,100
  if not keep_running
    break
}
keep_running := false
return

#IfWinActive

Click_Screen(x, y){
  CoordMode, Mouse, Screen
  Click, down, %x% %Y%
  Sleep 100
  Click, up
}