-- dump-fds-ram.lua: Dumps all 2048 bytes of NES RAM ($0000-$07FF) after every frame.
-- Output path is read from a sidecar file: tools/dump-output-path.txt
-- Usage:
--   echo "/abs/path/to/output.bin" > tools/dump-output-path.txt
--   fceux --playmov movie.fm2 --loadlua tools/dump-fds-ram.lua rom.fds

local path_file = io.open("tools/dump-output-path.txt", "r")
    or io.open("/Users/jivie/Projects/smb-translation/tools/dump-output-path.txt", "r")
local output_path = path_file and path_file:read("*l") or "/tmp/fceux-ram-dump.bin"
if path_file then path_file:close() end

local outfile, err = io.open(output_path, "wb")
if not outfile then print("ERROR: Cannot open " .. output_path .. " -- " .. (err or "?")); return end

-- Run at maximum speed (no frame rate cap)
emu.speedmode("maximum")

local frame_count = 0
local movie_was_active = false

emu.registerafter(function()
    local bytes = {}
    for i = 0, 2047 do bytes[i + 1] = string.char(memory.readbyte(i)) end
    outfile:write(table.concat(bytes))
    frame_count = frame_count + 1
    if frame_count % 1000 == 0 then outfile:flush(); print("Dumped " .. frame_count .. " frames") end
    if movie.active() then movie_was_active = true
    elseif movie_was_active then
        outfile:close(); outfile = nil
        print("Done: " .. frame_count .. " frames to " .. output_path)
        emu.exit()
    end
end)

emu.registerexit(function() if outfile then outfile:close() end end)
print("RAM dump -> " .. output_path)
