-- Copyright (C) 2016 Pau Carré Cardona - All Rights Reserved
-- You may use, distribute and modify this code under the
-- terms of the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt).

local torchFolder = require('paths').thisfile('..')
package.path = string.format("%s;%s/?.lua", os.getenv("LUA_PATH"), torchFolder)

require 'inn'
require 'optim'
require 'xlua'
require 'lfs'
local torch = require 'torch'

local tiefvision_config_loader = require '0-tiefvision-commons/tiefvision_config_loader'
local search_commons = require '10-similarity-searcher-cnn-db/search_commons'
local database = tiefvision_config_loader.load().database.unsupervised_similarity

local function getTestError(reference)
  local similarities = database:read(reference)

  local comparisonTable = {}
  for file, sim in pairs(similarities) do
    table.insert(comparisonTable, { file, sim })
  end

  table.sort(comparisonTable, search_commons.sortCmpTable)
  search_commons.printCmpTable(comparisonTable)
end

local function getOptions()
  local cmd = torch.CmdLine()
  cmd:text()
  cmd:text('Unsupervised image search from precomputed database of distances between each pair of images in the master folder.')
  cmd:text('Returns a descending sorted list of filenames concatenated with a similarity metric.')
  cmd:text('Both the filename to search and the result filenames come from the folder $TIEFVISION_HOME/resources/dresses-db/master.')
  cmd:text()
  cmd:text('Options:')
  cmd:argument('image', 'Filename (not full path, just the filename) from $TIEFVISION_HOME/resources/dresses-db/master.', 'string')
  cmd:text()
  cmd:option('-config', tiefvision_config_loader.default, 'Configuration file to use.')
  cmd:text()
  return cmd:parse(arg)
end

local options = getOptions()
getTestError(options.image)
