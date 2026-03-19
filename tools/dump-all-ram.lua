-- dump-all-ram.lua: Dumps all 2048 bytes of NES RAM ($0000-$07FF) after every frame.
-- Output: a flat binary file where each frame is 2048 consecutive bytes.
--
-- Usage:
--   fceux --playmov movie.fm2 --loadlua tools/dump-all-ram.lua smb.nes

local output_path = "/tmp/fceux-full-ram.bin"
local outfile, err = io.open(output_path, "wb")
if not outfile then
    print("ERROR: Cannot open output file: " .. output_path .. " -- " .. (err or "unknown"))
    return
end

local frame_count = 0
local movie_was_active = false

emu.registerafter(function()
    local bytes = {}
    for i = 0, 2047 do
        bytes[i + 1] = string.char(memory.readbyte(i))
    end
    outfile:write(table.concat(bytes))
    frame_count = frame_count + 1
    if frame_count % 1000 == 0 then
        print("Dumped " .. frame_count .. " frames")
    end

    if movie.active() then
        movie_was_active = true
    elseif movie_was_active then
        print("Movie ended at frame " .. frame_count)
        outfile:close()
        outfile = nil
        print("Wrote " .. frame_count .. " frames (" .. (frame_count * 2048) .. " bytes) to " .. output_path)
        emu.exit()
    end
end)

emu.registerexit(function()
    if outfile then
        outfile:close()
    end
    print("Final: " .. frame_count .. " frames to " .. output_path)
end)

print("RAM dump started -> " .. output_path)
