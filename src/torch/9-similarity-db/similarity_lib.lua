-- Copyright (C) 2016 Pau Carré Cardona - All Rights Reserved
-- You may use, distribute and modify this code under the
-- terms of the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt).

require 'torch'

local similarity_lib = {}

function similarity_lib.similarity(referenceEncoding, imageEncoding)
  local minHeight = math.min(referenceEncoding:size()[2], imageEncoding:size()[2])
  local maxHeight = math.max(referenceEncoding:size()[2], imageEncoding:size()[2])
  if (maxHeight - minHeight < 5) then
    local reshapedReference = referenceEncoding:clone()
    local reshapedImage = imageEncoding:clone()
    reshapedReference:resize(384 * minHeight * referenceEncoding:size()[1])
    reshapedImage:resize(384 * minHeight * imageEncoding:size()[1])
    local similarity = torch.dot(reshapedReference, reshapedImage) / (referenceEncoding:size()[1] * minHeight)
    return similarity
  else
    return -1.0
  end
end

function similarity_lib.similarity2(referenceEncoding, imageEncoding)
  local similarity = -1
  local taller = referenceEncoding:clone()
  local shorter = imageEncoding:clone()
  if referenceEncoding:size()[2] < imageEncoding:size()[2] then
    local tmp = taller
    taller = shorter
    shorter = tmp
  end
  local diff = taller:size()[2] - shorter:size()[2]
  local minHeight = shorter:size()[2]
  local width = shorter:size()[1]
  shorter:resize(width * minHeight * 384)
  if diff == 0 then
    return similarity_lib.similarity(referenceEncoding, imageEncoding)
  end
  for d = 1, diff do
    local sub = taller:clone():sub(1, width, d, d + minHeight - 1)
    sub:resize(width * minHeight * 384)
    local partialSimilarity = torch.dot(shorter, sub) / (width * minHeight)
    if partialSimilarity > similarity then
      similarity = partialSimilarity
    end
  end
  return similarity
end

return similarity_lib
