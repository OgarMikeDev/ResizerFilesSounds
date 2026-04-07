import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;

/*
TODO
 Main получает из src_folder аудио-файлы,
 сохраняет их в массив всех аудиофайлов,
 делит этот массив на 2 части,
 в первой части первые 2 аудиофайла,
 во второй части последние 2 аудиофайла,
 передаёт обе части аудиофайлов
 объекту класса ChangeQualityAudioFiles
 */
public class MainSimple {
    private static final String pathToSrcFolder = "src/main/resources/data/src_folder";
    private static final String pathToDstFolder = "src/main/resources/data/dst_folder";
    public static void main(String[] args) {
        //TODO Создание папки для исходных аудиофайлов
        File audioFileSrcFolder = new File(pathToSrcFolder);
        //TODO Массив аудиофайлов
        File[] allAudioFiles = audioFileSrcFolder.listFiles();
        //TODO Разделение аудиофайлов на 2 части для 2-х потоков
        int middle = allAudioFiles.length / 2;
        //TODO Первая половина аудиофайлов
        File[] firstPartAudioFiles = new File[middle];
        //TODO Заполнение firstPartAudioFiles первой частью аудиофайлов
        System.arraycopy(allAudioFiles, 0, firstPartAudioFiles, 0, firstPartAudioFiles.length);
        ChangeQualityAudioFiles firstChangeQualityAudioFiles = new ChangeQualityAudioFiles(
                firstPartAudioFiles, pathToDstFolder
        );
        new Thread(firstChangeQualityAudioFiles).start();

        //TODO Вторая половина аудиофайлов
        File[] secondPartAudioFiles = new File[allAudioFiles.length - firstPartAudioFiles.length];
        //TODO Заполнение secondPartAudioFiles второй частью аудиофайлов
        System.arraycopy(allAudioFiles, middle, secondPartAudioFiles, 0, secondPartAudioFiles.length);
        ChangeQualityAudioFiles secondChangeQualityAudioFiles = new ChangeQualityAudioFiles(
                secondPartAudioFiles,
                pathToDstFolder
        );
        new Thread(secondChangeQualityAudioFiles).start();
    }
}

/*
TODO
  ChangeQualityAudioFiles принимает текущую часть аудиофайлов,
  проходит по каждому из них,
  копирует и преобразовывает(сжимает)
  и сохраняет в dst_folder
 */
class ChangeQualityAudioFiles extends Thread {
    private File[] partOriginalAudioFiles;
    private String pathToDstFolder;

    public ChangeQualityAudioFiles(File[] partOriginalAudioFiles, String pathToDstFolder) {
        this.partOriginalAudioFiles = partOriginalAudioFiles;
        this.pathToDstFolder = pathToDstFolder;
    }

    @Override
    public void run() {
        try {
            //TODO originalAudioFile - сам файл в виде File(как закрытая книга на полке)
            for (File originalAudioFile : partOriginalAudioFiles) {
                /*
                TODO
                  originalAudioInputStream - открытый поток данных из originalAudioFile,
                  чтобы можно было читать содержимое
                  (как открытая книга на полке, кот-ю можно читать)
                 */
                AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(originalAudioFile);
                /*
                TODO
                 Как часто нужно измерять звук(частота).
                 Сколько места занимает одно измерение(разрядность).
                 Используется для одного или обоих ух(каналы).
                 originalAudioFormat описывает,
                 что измерять звук нужно определённое кол-во раз(44_100) в секунду
                 */
                AudioFormat originalAudioFormat =  originalAudioInputStream.getFormat();
                /*
                TODO
                 Новый формат с половинной частотой.
                 За счёт newAudioFormat "новый" аудиофайл
                 будет иметь размер в 2 раза меньше,
                 но длиться столько же по времени,
                 сколько и исходный.
                 newAudioFormat описывает,
                 что измерять звук нужно в 2 раза меньше(22_050) раз в секунду
                 */
                AudioFormat newAudioFormat = new AudioFormat(
                        originalAudioFormat.getEncoding(), //TODO Тип кодирования
                        /*
                        TODO
                         Частота дискретизации.
                         Частота дискретизации для звука -
                         тоже самое, что частота кадров для видео.
                         Сколько отдельных измерений в секундщу.
                         Меняем частоту:
                         было 44_100, стало 22_050.
                         Для стерео звука одно измерение -
                         это одно число для левого уха +
                         одно число для правого уха
                         */
                        originalAudioFormat.getSampleRate() / 2,
                        originalAudioFormat.getSampleSizeInBits(), //TODO Разрядность(бит на симпл)
                        originalAudioFormat.getChannels(), //TODO Кол-во каналов
                        /*
                        TODO
                         Размер фрейма
                         (совокупность сэмплов за 1 секунду)
                         в байтах
                         */
                        originalAudioFormat.getFrameSize(),
                        /*
                        TODO
                         Частота фреймов.
                         Меняем кол-во измерений в секунду:
                         тоже в 2 раза меньше.
                         Это сколько наборов измерений в секунду
                         */
                        originalAudioFormat.getFrameRate() / 2,
                        originalAudioFormat.isBigEndian() //TODO Порядок байтов
                );

                /*
                TODO
                  newAudioInputStream - что мы сохраняем в конечном виде.\
                  Преобразование(прореживание сэмплов).
                  Чтение оригинальных сэмплов;
                  Отбрасывание каждого второго(прореживние);
                  Создание нового потока с изменённым форматом
                  //Уменьшённая частота(каждый второй сэмпл)
                  for (int i = 0; i < originalSamples.length; i++) {
                        newSamples[i] = originalSamples[i * 2]; //берём с шагом 2
                  }
                 */
                AudioInputStream newAudioInputStream = AudioSystem.getAudioInputStream(newAudioFormat, originalAudioInputStream);
                //TODO Куда сохранять(путь, по кот-му происходит дальнейшее сохранение нового аудиофайла)
                File newAudioFile = new File(pathToDstFolder + "/" + originalAudioFile.getName());
                //TODO Само сохранение нового аудиофайла(newAudioInputStream) в формате WAVE по пути newAudioFile
                AudioSystem.write(newAudioInputStream, AudioFileFormat.Type.WAVE, newAudioFile);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
