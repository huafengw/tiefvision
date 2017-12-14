-- Copyright (C) 2016 Pau Carré Cardona - All Rights Reserved
-- You may use, distribute and modify this code under the
-- terms of the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt).

local torchFolder = require('paths').thisfile('..')
package.path = string.format("%s;%s/?.lua", os.getenv("LUA_PATH"), torchFolder)

require 'inn'
require 'optim'
require 'xlua'
local image = require 'image'
local torch = require 'torch'

local bboxlib = require '6-bboxlib/bboxlib'

local function getOptions()
    local cmd = torch.CmdLine()
    cmd:text()
    cmd:text('Get Bounding Box')
    cmd:text()
    cmd:text('Options:')
    cmd:argument('image', 'Image file path', 'string')
    cmd:argument('dest', 'Destine path to save the cropped image file', 'string')
    cmd:text()
    return cmd:parse(arg)
end

local function isNan(num)
    return num ~= num
end

local function getBoundingBox2(fileName, dest)
    local input = bboxlib.loadImageFromFile(fileName)
    local bboxes = bboxlib.getImageBoundingBoxesTable(input, 1)
    for i = 1, #bboxes do
        local xmin = bboxes[i][1]
        local ymin = bboxes[i][2]
        local xmax = bboxes[i][3]
        local ymax = bboxes[i][4]
        if not isNan(xmin) then
            print(xmin, ymin, xmax, ymax)
            local inputCropped = image.crop(input, xmin, ymin, xmax, ymax)
            image.save(dest, inputCropped)

            collectgarbage()
        end
    end
end

local options = getOptions()
getBoundingBox2(options.image, options.dest)

