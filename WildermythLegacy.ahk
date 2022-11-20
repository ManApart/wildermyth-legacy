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
  Click_screen(0.4166667, 0.3074)
  Sleep 100

  Send, 7
  Sleep 100

Loop 700 
{
  ;Export Character
  Click_Screen(.475,.5925)
  Sleep,200

  ;Click back in
  Click_Screen(.47812, .34259)
  Sleep,100

  ;Next Hero
  Click_Screen(.29166, .10185)
  Sleep,100
  if not keep_running
    break
}
keep_running := false
return

#IfWinActive

Click_Screen(x, y){
  CoordMode, Mouse, Screen
  absX := A_ScreenWidth * x
  absY :=  A_ScreenHeight * y

;  ListVars
;  Pause

  Click, down, %absX% %absY%
  Sleep 100
  Click, up
}