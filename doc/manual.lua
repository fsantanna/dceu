-- lua manual.lua manual.md > manual-out.md

local S1 = {
    -- [1] = {
    --    n   = 1
    --    sec = '1.',
    --    tit = "XXX",
    --    [1] = { n=2, sec='1.', tit="YYY", [1]={...}, ... },
    --    [2] = { ... },
    -- }
}

local S2 = {
    -- ["XXX"] = '1.'
    -- ["YYY"] = '1.1.'
}

local S3 = {
    -- { '1.',   "XXX" },
    -- { '1.1.', "YYY" },
}

do
    local N = {
        0, 0, 0, 0, 0, 0,
        -- [1] = i      -- head #  is on n=i
        -- [2] = j      -- head ## is on n=j
    }

    local CONTENTS = false         -- ignore title

    for l in io.lines(...) do
        if l == "<!-- CONTENTS -->" then
            CONTENTS = true
        end

        local n, tit = string.match(l, '^(#+) (.+)$')
        if CONTENTS and n then
            n = string.len(n)
            assert(N[n], "too many levels")
            N[n] = N[n] + 1
            for i=n+1, #N do
                N[i] = 0        -- reset further levels
            end

            local pre = ''      -- 2.1.3.
            for i=1, n do
                pre = pre .. N[i] .. '.'
            end
            assert(not S2[tit], tit)  -- duplicate title
            S2[tit] = pre
            if n < 3 then
                S3[#S3+1] = { pre, tit }
            end

            --print(n, pre, tit)

            local T = S1
            for i=1, n-1 do
                T = T[N[i]]
            end
            assert(not T[N[n]])
            T[N[n]] = { n=n, sec=N[n]..'.', tit=tit }
        end
    end
end

--[[
function summary (T)
    for i=1, #T do
        local t = T[i]
        print(string.rep(' ',4*(t.n-1)) .. t.sec .. ' ' .. t.tit)
        summary(t)
    end
end
summary(S1)
]]

function tolink (s)
    return string.lower(string.gsub(string.gsub(s,"[%p]",""), "%s+", "-"))
end

do
    local CONTENTS = false         -- ignore title
    local s3 = 1

    for l in io.lines(...) do
        if l == "<!-- CONTENTS -->" then
            CONTENTS = true
        end

        local n1, tit1 = string.match(l, '^( *)%* (.+)$')
        local n2, tit2 = string.match(l, '^(#+) (.+)$')
        if not CONTENTS and n1 then
            --print('>', S3[s3][2], tit1)
            assert(S3[s3][2] == tit1)
            print(n1..'- <a href="#'..tolink(tit1)..'">'..S3[s3][1]..'</a> '..tit1)
            s3 = s3 + 1
        elseif CONTENTS and n2 then
            print('<a name="'..tolink(tit2)..'"/>')
            print()
            print(n2 .. ' ' .. S2[tit2] .. ' ' .. tit2)
        else
            print(l)
        end
    end

    assert(s3 == #S3+1)
end
