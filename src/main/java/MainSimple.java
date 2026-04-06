import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;

public class MainSimple {
    private static final String pathToSrcFolder = "src/main/resources/data/src_folder_sound";
    private static final String pathToDstFolder = "src/main/resources/data/dst_folder_sound";

    public static void main(String[] args) {
        //TODO Создаём папку для исходных файлов
        File fileSrcFolder = new File(pathToSrcFolder);
        System.out.println("Папка с аудиофайлами: " + fileSrcFolder);

        File[] filesAllSounds = fileSrcFolder.listFiles();
        System.out.println("Массив аудиофайлов: " + filesAllSounds);

        //TODO Разделяем файлы на 2 части для 2-х потоков
        int middle = filesAllSounds.length / 2;

        //TODO Первая половина файлов
        File[] filesSounds1 = new File[middle];
        System.arraycopy(filesAllSounds, 0, filesSounds1, 0, filesSounds1.length);
        for (File fileIn1Part : filesSounds1) {
            System.out.println("Название текущего файла: " + fileIn1Part.getName());
        }

        //TODO TODO Вторая половина файлов
        File[] filesSounds2 = new File[filesAllSounds.length - filesSounds1.length];
        System.arraycopy(filesAllSounds, middle, filesSounds2, 0, filesSounds2.length);

        //TODO Создаём и запускаем 1-й поток
        ChangeQualitySounds changeQualitySounds1 = new ChangeQualitySounds(pathToDstFolder, filesSounds1);
        new Thread(changeQualitySounds1).start();

        //TODO Создаём и запускаем 2-й поток
        ChangeQualitySounds changeQualitySounds2 = new ChangeQualitySounds(pathToDstFolder, filesSounds2);
        new Thread(changeQualitySounds2).start();
    }
}

/*
TODO
    Сэмпл - измерение звука в определённый промежуток времени.
    Программа убирает каждый второй сэмпл,
    но время между оставшимися сэмплами увеличивает в 2.
    Почему звук по длине не меняется:
    длительность =
    кол-во сэмплов(в исх-й мелодии 44_100, в новой 22_050) /
    частоту дискретизации(в исх-й мелодии 44_100 Гц, в новой 22_050 Гц)
 */
class ChangeQualitySounds extends Thread {
    private String pathToDstFolder;
    private File[] filesPartSounds;

    public ChangeQualitySounds(String pathToDstFolder, File[] filesPartSounds) {
        this.pathToDstFolder = pathToDstFolder;
        this.filesPartSounds = filesPartSounds;
    }

    @Override
    public void run() {
        try {
            for (File currentFileSound : filesPartSounds) {
                AudioInputStream originAudioInputStream = AudioSystem.getAudioInputStream(currentFileSound);
                /*
                TODO
                 Как часто нужно измерять звук(частота).
                 Сколько места занимает одно измерение(разрядность).
                 Это для одного уха или для двух(каналы).
                 originAudioFormat описывает,
                 что измерять звук нужно 44_100 раз в секунду.
                 */
                AudioFormat originAudioFormat = originAudioInputStream.getFormat();

                /*
                TODO
                 Новый формат с половинной частотой.
                 За счёт newAudioFormat "новая" мелодия
                 будет иметь размер в 2 раза меньше,
                 но длиться столько же по времени,
                 сколько и исходная.
                 newAudioFormat описывает,
                 что измерять звук нужно 22_050 раз в секунду.
                 */
                AudioFormat newAudioFormat = new AudioFormat(
                        originAudioFormat.getEncoding(), //TODO Тип кодирования
                        /*
                        TODO
                         Частота дискретизации.
                         Частота дискретизации для звука - это то же самое,
                         что частота кадров для видео.
                         Сколько отдельных измерений в секунду.
                         Меняем частоту:
                         было 44_100, стало 22_050.
                         Для стерео звука одно измерение -
                         это одно число для левого уха +
                         одно число для правого уха
                         */


                        originAudioFormat.getSampleRate() / 2,
                        originAudioFormat.getSampleSizeInBits(), //TODO Разрядность(бит на симпл)
                        originAudioFormat.getChannels(), //TODO Кол-во каналов
                        originAudioFormat.getFrameSize(), //TODO Размер фрейма в байтах
                        /*
                        TODO
                         Частота фреймов.
                         Меняем кол-во измерений в секунду: тоже в 2 раза меньше.
                         Это сколько наборов измерений в секунду.
                         */
                        originAudioFormat.getFrameRate() / 2,
                        originAudioFormat.isBigEndian() //TODO Порядок байтов
                );

                /*
                TODO
                 Преобразование(прореживание сэмплов).
                 Читает оригинальные сэмплы.;
                 Отбрасывает каждый второй(прореживание);
                 Создаёт новый поток с изменённым форматом.
                 //Оригинальные сэмплы(каждый)
                 for (int i = 0; i < originSamples.length; i++) {
                        newSamples[i] = originSamples[i];
                 }
                 //Уменьшённая частота(каждый второй)
                 for (int i = 0; i < originSamples.length; i++) {
                        newSamples[i] = originSamples[i * 2]; //берём с шагом 2
                 }
                 */
                AudioInputStream newAudioInputStream = AudioSystem.getAudioInputStream(newAudioFormat, originAudioInputStream);

                File newFileSound = new File(pathToDstFolder + "/" + currentFileSound.getName());
                AudioSystem.write(newAudioInputStream, AudioFileFormat.Type.WAVE, newFileSound);
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}