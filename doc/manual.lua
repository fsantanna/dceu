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

do
    local N = {
        0, 0, 0, 0, 0, 0,
        -- [1] = i      -- head #  is on n=i
        -- [2] = j      -- head ## is on n=j
    }

    local TITLE = false         -- ignore title

    for l in io.lines(...) do
        local n, tit = string.match(l, '^(#+) (.+)$')
        if TITLE and n then
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

            --print(n, pre, tit)

            local T = S1
            for i=1, n-1 do
                T = T[N[i]]
            end
            assert(not T[N[n]])
            T[N[n]] = { n=n, sec=N[n]..'.', tit=tit }
        end
        if n then
            TITLE = true
        end
    end
end

function summary (T)
    for i=1, #T do
        local t = T[i]
        print(string.rep(' ',4*(t.n-1)) .. t.sec .. ' ' .. t.tit)
        summary(t)
    end
end

summary(S1)

do
    local TITLE = false         -- ignore title

    for l in io.lines(...) do
        local n, tit = string.match(l, '^(#+) (.+)$')
        if TITLE and n then
            print(n .. ' ' .. S2[tit] .. ' ' .. tit)
        else
            print(l)
        end
        if n then
            TITLE = true
        end
    end

end
