-- Copyright (C) 2016 Pau Carré Cardona - All Rights Reserved
-- You may use, distribute and modify this code under the
-- terms of the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt).

--
-- Reader and writer to store information thanks to torch
--

local paths = require('paths')
local torchFolder = paths.thisfile('..')
package.path = string.format("%s;%s/?.lua", os.getenv("LUA_PATH"), torchFolder)

local torch = require 'torch'

local tiefvision_commons = require '0-tiefvision-commons/tiefvision_commons'
local Tiefvision_torch_io = {}
Tiefvision_torch_io.__index = Tiefvision_torch_io

setmetatable(Tiefvision_torch_io, {
  __call = function (cls, path)
    return cls.new(path)
  end,
})

function Tiefvision_torch_io.new(path)
  local self = setmetatable({}, Tiefvision_torch_io)
  self.folder = path
  return self
end

function Tiefvision_torch_io:read(key)
  local file = tiefvision_commons.path(self.folder, key)
  if not paths.filep(file) then
    return nil
  end

  return torch.load(file)
end

function Tiefvision_torch_io:write(key, value)
  local file = tiefvision_commons.path(self.folder, key)

  paths.mkdir(paths.dirname(file))
  torch.save(file, value)
end

function Tiefvision_torch_io:keys()
  local files = {}
  for file in paths.files(self.folder) do
    files[#files + 1] = file
  end

  return files
end

return Tiefvision_torch_io
