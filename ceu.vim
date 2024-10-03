" Vim syntax file
" Language:     Ceu
" Maintainer:   Francisco Sant'Anna <francisco.santanna@gmail.com>
" Last Change:  2024 October

if exists("b:current_syntax")
    finish
endif

syn iskeyword 33,39,45,63,97-122

syn match   Comment   ";;.*$"
syn region  Comment   start=";;;[^;]" end=";;;[^;]"

syn region  String    start=/\v"/ skip=/\v(\\[\\"]){-1}/ end=/\v"/
syn match   String    "'.'"
"syntax region String start=/\v'/ skip=/\v(\\[\\"]){-1}/ end=/\v'/

syn match   Constant  '\d\+'
syn keyword Constant  false nil true

syn keyword Function  drop dump error pub
syn keyword Function  next-dict print println
syn keyword Function  sup? tag to-string-number
syn keyword Function  to-string-pointer to-string-tag
syn keyword Function  to-tag-string tuple type next-tasks

syn keyword Function  not and or
syn keyword Function  is is-not
syn keyword Function  in in-not

syn keyword Function  assert
syn keyword Function  static? dynamic? string?
syn keyword Function  to random math tag-or next create-resume copy
syn keyword Function  pico

syn match   Type      ':[a-zA-Z0-9'?!\.\-]\+'

syn match   Statement '[\+\-\*\/\%\>\<\=\|\&\~]'

syn keyword Statement data defer do else group if set
syn keyword Statement val var catch escape resume
syn keyword Statement yield broadcast delay in it
syn keyword Statement spawn toggle tasks await
syn keyword Statement break coro enum every func
syn keyword Statement ifs loop match par par-and
syn keyword Statement par-or resume-yield-all
syn keyword Statement return skip task tasks test thus
syn keyword Statement until watching with where
syn keyword Statement while
syn keyword Statement coroutine status

syn keyword Todo      TODO FIXME XXX
syn region  String    start='"' end='"'
