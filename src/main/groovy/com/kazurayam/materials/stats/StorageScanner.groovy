package com.kazurayam.materials.stats

import java.nio.file.Path

import javax.imageio.ImageIO
import java.util.concurrent.TimeUnit

import groovy.json.JsonOutput

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.lang3.time.StopWatch

import com.kazurayam.imagedifference.ImageDifference
import com.kazurayam.materials.FileType
import com.kazurayam.materials.ImageDeltaStats
import com.kazurayam.materials.Material
import com.kazurayam.materials.MaterialStorage
import com.kazurayam.materials.TSuiteName
import com.kazurayam.materials.TSuiteResult
import com.kazurayam.materials.TSuiteResultId

class StorageScanner {
    
    static Logger logger_ = LoggerFactory.getLogger(StorageScanner.class)
    
    private MaterialStorage materialStorage_
    private Options options_
    
    public StorageScanner(MaterialStorage materialStorage) {
        this(materialStorage, new Options.Builder().build())
    }
    
    public StorageScanner(MaterialStorage materialStorage, Options options) {
        this.materialStorage_ = materialStorage
        this.options_ = options
    }
    
    /**
     * This will return
     * <PRE>
     * {
     *  "defaultCriteriaPercentage":5.0,
     *  "statsEntryList":[
     *      // a StatsEntry object of the TSuiteName specified
     *  ]
     * }
     * </PRE>
     *
     * @param materialStorage
     * @return a ImageDeltaStats object
     */
    ImageDeltaStats scan(TSuiteName tSuiteName) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        
        ImageDeltaStatsImpl.Builder builder = 
            new ImageDeltaStatsImpl.Builder().
                defaultCriteriaPercentage(options_.getDefaultCriteriaPercentage())
        if (materialStorage_.getTSuiteNameList().contains(tSuiteName)) {
            StatsEntry se = this.makeStatsEntry(tSuiteName)
            builder.addImageDeltaStatsEntry(se)
        } else {
            logger_.warn("No ${tSuiteName} is found in ${materialStorage_}")
        }
        ImageDeltaStats ids = builder.build()
        
        stopWatch.stop()
        logger_.debug("#scan(${tSuiteName}) took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return ids
    }
    
    /**
     * This will return ...
     * <PRE>
     * {
     *  "defaultCriteriaPercentage":5.0,
     *  "statsEntryList":[
     *      // list of StatsEntry objects
     *  ]
     * }
     * </PRE>
     * @deprecated It takes long if you scan multiple TSuiteName. Do not use this. Be specific to process a single TSuiteName.
     * @param materialStorage
     * @return a ImageDeltaStats object
     */
    /*
    ImageDeltaStats scan() {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        ImageDeltaStatsImpl.Builder builder = new ImageDeltaStatsImpl.Builder().
                                defaultCriteriaPercentage(5.0)
        for (TSuiteName tSuiteName : materialStorage_.getTSuiteNameList()) {
            StatsEntry se = this.makeStatsEntry(tSuiteName)
            builder.addImageDeltaStatsEntry(se)
            logger_.info("#scan created StatsEntry of ${se.getTSuiteName()}")
        }
        stopWatch.stop()
        logger_.debug("#scan() took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return builder.build()
    }
    */
    
    /**
     * This will return
     * <PRE>
     *     {
     *          "TSuiteName": "47News_chronos_capture",
     *          "materialStatsList": [
     *              // list of MaterialStats objects
     *          ]
     *     }
     * </PRE>
     * 
     * @param ms
     * @param tSuiteName
     * @return a StatsEntry object
     */
    StatsEntry makeStatsEntry(TSuiteName tSuiteName) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        StatsEntry statsEntry = new StatsEntry(tSuiteName)
        Set<Path> set = 
            materialStorage_.getSetOfMaterialPathRelativeToTSuiteTimestamp(tSuiteName)
        for (Path path : set) {
            MaterialStats materialStats = this.makeMaterialStats(tSuiteName, path)
            statsEntry.addMaterialStats(materialStats)
        }
        stopWatch.stop()
        logger_.debug("#makeStatsEntry(${tSuiteName}) took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return statsEntry
    }

    
    /**
     * This will return
     * <PRE>
     *              {
     *                  "path: "main.TC_47News.visitSite/47NEWS_TOP.png",
     *                  "imageDeltaList": [
     *                      // list of ImageDelta objects
     *                  ],
     *                  "calculatedCriteriaPercentage": 2.51
     *              }
     * </PRE>
     * 
     * @param ms
     * @param tSuiteName
     * @return
     */
    MaterialStats makeMaterialStats(TSuiteName tSuiteName,
                                Path pathRelativeToTSuiteTimestamp) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        // at first, look up materials of FileType.PNG 
        //   within the TSuiteName across multiple TSuiteTimestamps
        List<Material> materials = this.getMaterialsOfARelativePathInATSuiteName(
                                        tSuiteName,
                                        pathRelativeToTSuiteTimestamp)
        
        // sort the Material list by the descending order of TSuiteTimestamp
        Collections.sort(materials, new Comparator<Material>() {
            public int compare(Material materialA, Material materialB) {
                TSuiteResult tsrA = materialA.getTCaseResult().getParent()
                TSuiteResult tsrB = materialB.getTCaseResult().getParent()
                if (tsrA > tsrB) {
                    return -1
                } else if (tsrA == tsrB) {
                    Path pathA = materialA.getPath()
                    Path pathB = materialB.getPath()
                    return pathA.compareTo(pathB)
                } else {
                    return 1
                }
            }
        })

        // build the MaterialStats object while calculating the diff ratio 
        // of two PNG files
        List<ImageDelta> imageDeltaList = new ArrayList<ImageDelta>()
        if (materials.size() > 1) {
            for (int i = 0; i < materials.size() - 1; i++) {
                ImageDelta imageDelta = StorageScanner.makeImageDelta(
                                    materials.get(i), materials.get(i + 1))
                imageDeltaList.add(imageDelta)
            }
        }
        MaterialStats materialStats  = new MaterialStats(
                    pathRelativeToTSuiteTimestamp, imageDeltaList)
        // configure parameters
        materialStats.setFilterDataLessThan(options_.getFilterDataLessThan())
        
        //
        stopWatch.stop()
        logger_.debug("#makeMaterialStats(${tSuiteName},${pathRelativeToTSuiteTimestamp} " + 
            "took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return materialStats
    }

    /**
     *
     * @param ms
     * @param tSuiteName
     * @param pathRelativeToTSuiteTimestamp
     * @return
     */
    List<Material> getMaterialsOfARelativePathInATSuiteName(
                                TSuiteName tSuiteName,
                                Path pathRelativeToTSuiteTimestamp) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        List<Material> materialList = new ArrayList<Material>()
        //
        List<TSuiteResultId> idsOfTSuiteName = materialStorage_.getTSuiteResultIdList(tSuiteName)
        for (TSuiteResultId tSuiteResultId : idsOfTSuiteName) {
            TSuiteResult tSuiteResult = materialStorage_.getTSuiteResult(tSuiteResultId)
            for (Material mate: tSuiteResult.getMaterialList()) {
                if (mate.fileType.equals(FileType.PNG) &&
                    mate.getPathRelativeToTSuiteTimestamp() ==
                            pathRelativeToTSuiteTimestamp) {
                    materialList.add(mate)
                }
            }
        }
        stopWatch.stop()
        logger_.debug("#getMaterialsOfARelativePathInATSuiteName(${tSuiteName},${pathRelativeToTSuiteTimestamp} " +
            "took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return materialList
    }

    /**
     * Read 2 PNG files to get image difference, calculate the diff ratio,
     * and will return a ImageDelta object.
     * 
     * Please note that this method call takes fairly long processing time (2 to 4 seconds).
     * 
     * <PRE>
     *      { "a": "20190216_064354", "b": "20190216_064149", "delta": 0.10 }
     * </PRE>
     * 
     * @param a
     * @param b
     * @return a ImageDelta object
     */
    static ImageDelta makeImageDelta(Material a, Material b) {
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        Objects.requireNonNull(a, "Material a must not be null")
        Objects.requireNonNull(b, "Material b must not be null")
        if (a.getFileType() != FileType.PNG) {
            throw new IllegalArgumentException("${a.path()} is not a PNG file")
        }
        if (b.getFileType() != FileType.PNG) {
            throw new IllegalArgumentException("${b.path()} is not a PNG file")
        }
        // read PNG files and
        // create ImageDifference of the 2 given images to calculate the diff ratio
        ImageDifference diff = new ImageDifference(
                ImageIO.read(a.getPath().toFile()),
                ImageIO.read(b.getPath().toFile()))
        // make the delta
        ImageDelta imageDelta = new ImageDelta(
                                a.getParent().getParent().getTSuiteTimestamp(),
                                b.getParent().getParent().getTSuiteTimestamp(),
                                diff.getRatio())
        stopWatch.stop()
        logger_.debug("#makeImageDelta(${a}, ${b}) " +
            "took ${stopWatch.getTime(TimeUnit.MILLISECONDS)} milliseconds")
        return imageDelta
    }
    
    
    /**
     * 
     */
    static class Options {
        
        private double defaultCriteriaPercentage
        private double filterDataLessThan
        private double probability
        
        static class Builder {
            private double defaultCriteriaPercentage
            private double filterDataLessThan
            private double probability
            Builder() {
                this.defaultCriteriaPercentage = ImageDeltaStatsImpl.SUGGESTED_CRITERIA_PERCENTAGE
                this.filterDataLessThan = MaterialStats.FILTER_DATA_LESS_THAN
                this.probability = MaterialStats.PROBABILITY
            }
            Builder defaultCriteriaPercentage(double value) {
                if (value < 0.0) {
                    throw new IllegalArgumentException("defaultCriteriaPercentage must not be negative")
                }
                if (value > 100.0) {
                    throw new IllegalArgumentException("defaultCriteriaPercentage must not be  > 100.0")
                }
                this.defaultCriteriaPercentage = value
                return this
            }
            Builder filterDataLessThan(double value) {
                if (value < 0.0) {
                    throw new IllegalArgumentException("filterDataLessThan must not be negative")
                }
                if (value > 100.0) {
                    throw new IllegalArgumentException("filterDataLessThan must not be  > 100.0")
                }
                this.filterDataLessThan = value
                return this
            }
            Builder probability(double value) {
                if (value < 0.0) {
                    throw new IllegalArgumentException("probability must not be negative")
                }
                if (value > 1.0) {
                    throw new IllegalArgumentException("probability must not be > 1.0")
                }
                this.probability = value
                return this
            }
            Options build() {
                return new Options(this)
            }
        }
        
        private Options(Builder builder) {
            this.defaultCriteriaPercentage = builder.defaultCriteriaPercentage
            this.filterDataLessThan = builder.filterDataLessThan
            this.probability = builder.probability
        }
        
        double getDefaultCriteriaPercentage() {
            return this.defaultCriteriaPercentage
        }
        
        double getFilterDataLessThan() {
            return this.filterDataLessThan
        }
        
        double getProbability() {
            return this.probability
        }
        
        @Override
        String toString() {
            return JsonOutput.toJson(this)
        }
    }
}
