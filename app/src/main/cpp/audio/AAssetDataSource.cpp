//
// Created by 43975 on 12/24/2021.
//
#include "../utils/logging.h"
#include "oboe/Oboe.h"
#include "AAssetDataSource.h"

#if !defined(USE_FFMPEG)
#error USE_FFMPEG should be defined in app.gradle
#endif

#if USE_FFMPEF==1
    #include "FFMpegExtractor.h"
#else
    #include "NDKExtractor.h"

#endif


constexpr int kMaxCompressionRatio{12};

AAssetDataSource* AAssetDataSource::newFromCompressedAsset(AAssetManager &assetManager,
        const char *filename,
        const AudioProperties targetProperties) {

    // get the asset by filename via AAssetManager
    AAsset *asset = AAssetManager_open(&assetManager, filename, AASSET_MODE_UNKNOWN);
    if (!asset)
    {
        LOGE("Failed to open asset %s",filename);
        return nullptr;
    }

    off_t assetSize = AAsset_getLength(asset);
    LOGD("Opened %s, size %ld",filename,assetSize);

    // Allocate memory to store decompressed audio. We don't know the exact
    // size of the decoded data until after decoding so we make an assumption about the
    // maximum compression ratio and the decoded sample format (float for FFmpeg, int16 for NDK)

#if USE_FFMPEG==true
    const long maximumDataSizeInBytes = kMaxCompressionRatio * assetSize * sizeof(float);
    auto decodedData = new uint8_t[maximumDataSizeInBytes];

    int64_t bytesDecoded = FFMpegExtractor::decode(asset, decodedData, targetProperties);
    auto numSamples = bytesDecoded / sizeof(float);

#else
    const long maximumDataSizeInBytes = kMaxCompressionRatio * assetSize  * sizeof(int16_t);
    auto decodedData = new uint8_t[maximumDataSizeInBytes];

    int64_t bytesDecoded = NDKExtractor::decode(asset,decodedData,targetProperties);
    auto numSamples = bytesDecoded / sizeof(int16_t);
#endif

    // Now we know the exact number of samples we can create a float array to hold the audio data
    auto outputBuffer  = std::make_unique<float[]>(numSamples);

#if USE_FFMPEF==1
    memcpy(outputBuffer.get(), decodedData, (size_t)bytesDecoded);
#else
    // The NDK decoder can only decode to int16, we need to convert to floats
    oboe::convertPcm16ToFloat(reinterpret_cast<int16_t*>(decodedData),outputBuffer.get(),
            bytesDecoded/sizeof(int16_t));
#endif

    delete [] decodedData;
    AAsset_close(asset);

    return new AAssetDataSource(std::move(outputBuffer), numSamples, targetProperties);

}

#include <fstream>

AAssetDataSource* AAssetDataSource::newFromPCM16Asset(AAssetManager &assetManager,
                                                           const char *filename,
                                                           const AudioProperties targetProperties) {
    // get the asset by filename via AAssetManager
    AAsset *asset = AAssetManager_open(&assetManager, filename, AASSET_MODE_UNKNOWN);
    if (!asset)
    {
        LOGE("Failed to open asset %s",filename);
        return nullptr;
    }

    off_t assetSize = AAsset_getLength(asset);
    LOGD("Opened %s, size %ld",filename,assetSize);

    auto numSamples = assetSize / sizeof(int16_t); // PCM16

    // Allocate memory to read your file
    char* fileContent = new char[assetSize+1];
// Read your file
    AAsset_read(asset, fileContent, assetSize);
// For safety you can add a 0 terminating character at the end of your file ...
    fileContent[assetSize] = '\0';

    // Now we know the exact number of samples we can create a float array to hold the audio data
    auto outputBuffer  = std::make_unique<float[]>(numSamples);

    // The NDK decoder can only decode to int16, we need to convert to floats
    oboe::convertPcm16ToFloat(reinterpret_cast<int16_t*>(fileContent),outputBuffer.get(),
                              numSamples);
    AAsset_close(asset);

    return new AAssetDataSource(std::move(outputBuffer), numSamples, targetProperties);
}

AAssetDataSource* AAssetDataSource::newFromFloat(AAssetManager &assetManager,
                                     const float *data,
                                     const size_t size,
                                     const AudioProperties targetProperties)
{
    off_t numSamples = size; // PCM16
    // Now we know the exact number of samples we can create a float array to hold the audio data
    auto outputBuffer  = std::make_unique<float[]>(numSamples);

    memcpy(outputBuffer.get(), data, size * sizeof(float));

    return new AAssetDataSource(std::move(outputBuffer), numSamples, targetProperties);
}