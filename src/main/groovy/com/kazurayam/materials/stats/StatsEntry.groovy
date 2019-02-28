package com.kazurayam.materials.stats

import java.nio.file.Path

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kazurayam.materials.Helpers
import com.kazurayam.materials.TSuiteName
import com.kazurayam.materials.TSuiteTimestamp

class StatsEntry {
    
    static Logger logger_ = LoggerFactory.getLogger(StatsEntry.class)
    
    static final StatsEntry NULL = new StatsEntry(null)

    private TSuiteName tSuiteName
    
    private List<MaterialStats> materialStatsList
    
    StatsEntry(TSuiteName tSuiteName) {
        this.tSuiteName = tSuiteName
        this.materialStatsList = new ArrayList<MaterialStats>() 
    }

    TSuiteName getTSuiteName() {
        return tSuiteName
    }
    
    void addMaterialStats(MaterialStats materialStats) {
        this.materialStatsList.add(materialStats)
    }
    
    List<MaterialStats> getMaterialStatsList() {
        return materialStatsList
    }
    
    MaterialStats getMaterialStats(Path path) {
        for (MaterialStats ms: materialStatsList) {
            if (ms.getPath().equals(path)) {
                return ms
            } else {
                logger_.warn("#getMaterialStats path:${path.toString()}, ms.getPath():${ms.getPath()}," +
                    " equals?:${ms.getPath().equals(path)}")
            }
        }
        return MaterialStats.NULL
    }
    
    boolean hasImageDelta(Path pathRelativeToTSuiteTimestamp, TSuiteTimestamp a, TSuiteTimestamp b) {
        for (MaterialStats ms: materialStatsList) {
            if (ms.getPath().equals(pathRelativeToTSuiteTimestamp) && ms.hasImageDelta(a, b)) {
                return true
            }
        }
        return false
    }
    
    ImageDelta getImageDelta(Path pathRelativeToTSuiteTimestamp, TSuiteTimestamp a, TSuiteTimestamp b) {
        for (MaterialStats ms: materialStatsList) {
            if (ms.getPath().equals(pathRelativeToTSuiteTimestamp) && ms.hasImageDelta(a, b)) {
                return ms.getImageDelta(a, b)
            }
        }
        return null
    }
    
    @Override
    String toString() {
        return this.toJsonText()
    }
    
    String toJsonText() {
        StringBuilder sb = new StringBuilder()
        sb.append("{")
        sb.append("\"TSuiteName\":")
        sb.append("\"${Helpers.escapeAsJsonText(tSuiteName.getValue())}\",")
        sb.append("\"materialStatsList\":")
        int count = 0
        sb.append("[")
        for (MaterialStats ms : materialStatsList) {
            if (count > 0) {
                sb.append(",")
            }
            sb.append(ms.toJsonText())
            count += 1
        }
        sb.append("]")
        sb.append("}")
        return sb.toString()
    }
    
    /**
     * <PRE>
     * {
            "TSuiteName": "47News_chronos_capture",
            "materialStatsList": [
                // list of MaterialStats objects
            ] 
     * }
     * </PRE>
     * @param json
     * @return
     */
    static StatsEntry fromJsonObject(Object jsonObject) {
        Objects.requireNonNull(jsonObject, "jsonObject must not be null")
        if (jsonObject instanceof Map) {
            Map statsEntryJsonObject = (Map)jsonObject
            if (statsEntryJsonObject.TSuiteName == null) {
                throw new IllegalArgumentException("map.TSuiteName must not be null")
            }
            if (statsEntryJsonObject.materialStatsList == null) {
                throw new IllegalArgumentException("map.materialStatsList must not be null")
            }
            StatsEntry statsEntry = new StatsEntry(new TSuiteName(statsEntryJsonObject.TSuiteName))
            for (Map entry : (List)statsEntryJsonObject.materialStatsList) {
                MaterialStats deserialized = MaterialStats.fromJsonObject(entry)
                statsEntry.addMaterialStats(deserialized)
            }
            return statsEntry
        } else {
            throw new IllegalArgumentException("jsonObject should be an instance of Map but was ${jsonObject.class.getName()}")
        }
    }
}
