package uk.ac.ebi.ena.cv19fd.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by raheela on 10/05/2021.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum DataTypeEnum {
    SEQUENCES(1, 1, 1, "Sequences", Arrays.asList(DownloadFormatEnum.EMBL, DownloadFormatEnum.FASTA)),
    REFERENCE_SEQUENCES(2, 1, 2, "Reference Sequences", Arrays.asList(DownloadFormatEnum.EMBL,
            DownloadFormatEnum.FASTA)),
    RAW_READS(3, 1, 3, "Raw Reads", Arrays.asList(DownloadFormatEnum.XML, DownloadFormatEnum.FASTQ,
            DownloadFormatEnum.SUBMITTED)),
    SEQUENCED_SAMPLES(4, 1, 4, "Sequenced Samples", Arrays.asList(DownloadFormatEnum.XML, DownloadFormatEnum.FASTQ,
            DownloadFormatEnum.SUBMITTED)),
    STUDIES(5, 1, 5, "Studies", Arrays.asList(DownloadFormatEnum.XML, DownloadFormatEnum.FASTQ,
            DownloadFormatEnum.SUBMITTED)),
    HUMAN_READS(6, 2, 1, "Human Reads", Arrays.asList(DownloadFormatEnum.XML, DownloadFormatEnum.FASTQ,
            DownloadFormatEnum.SUBMITTED)),
    OTHER_SPECIES_READS(7, 2, 2, "Other Species Reads", Arrays.asList(DownloadFormatEnum.XML,
            DownloadFormatEnum.FASTQ, DownloadFormatEnum.SUBMITTED));

    private int id;
    private int domainId;
    private int value;
    private String message;
    private List<DownloadFormatEnum> formats;
    private static Map<DomainEnum, Map<Integer, DataTypeEnum>> map = new HashMap<>();
    private static Map<String, DataTypeEnum> mapWithString = new HashMap<>();
    private static Map<Integer, DataTypeEnum> viralSequenceList = new HashMap<>();
    private static Map<Integer, DataTypeEnum> hostSequenceList = new HashMap<>();

    static {

        Arrays.stream(DataTypeEnum.values()).forEach(k -> {
            mapWithString.put(k.message, k);
            if (1 == k.getDomainId()) {
                viralSequenceList.put(k.value, k);
            } else {
                hostSequenceList.put(k.value, k);
            }

        });
        map.put(DomainEnum.VIRAL_SEQUENCES, viralSequenceList);
        map.put(DomainEnum.HOST_SEQUENCES, hostSequenceList);

    }


    public static DataTypeEnum valueOf(Integer domainId, Integer i) {
        if (DomainEnum.VIRAL_SEQUENCES.getValue() == domainId) {
            return viralSequenceList.get(i);
        }
        return hostSequenceList.get(i);
    }


}

