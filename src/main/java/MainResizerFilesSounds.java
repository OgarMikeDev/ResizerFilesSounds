import java.io.File;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class MainResizerFilesSounds {
    private static final String pathToSrcFolder = "src/main/resources/data/src_folder_sound";
    private static final String pathToDstFolder = "src/main/resources/data/dst_folder_sound";

    public static void main(String[] args) {
        // Создаём папку для исходных файлов
        File fileSrcFolder = new File(pathToSrcFolder);

        // Проверяем, существует ли папка с исходниками
        if (!fileSrcFolder.exists() || !fileSrcFolder.isDirectory()) {
            System.err.println("Ошибка: Папка с исходными файлами не найдена: " + pathToSrcFolder);
            return;
        }

        // Получаем все WAV файлы из папки
        File[] filesAllSounds = fileSrcFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".wav") ||
                        name.toLowerCase().endsWith(".aiff") ||
                        name.toLowerCase().endsWith(".au")
        );

        // Проверяем, есть ли файлы
        if (filesAllSounds == null || filesAllSounds.length == 0) {
            System.err.println("Ошибка: В папке нет поддерживаемых аудиофайлов (.wav, .aiff, .au)");
            return;
        }

        // Создаём папку для результатов, если её нет
        File dstFolder = new File(pathToDstFolder);
        if (!dstFolder.exists()) {
            dstFolder.mkdirs();
        }

        // Разделяем файлы на 2 части для двух потоков
        int middle = filesAllSounds.length / 2;

        // Первая половина файлов
        File[] filesSounds1 = new File[middle];
        System.arraycopy(filesAllSounds, 0, filesSounds1, 0, filesSounds1.length);

        // Вторая половина файлов
        File[] filesSounds2 = new File[filesAllSounds.length - middle];
        System.arraycopy(filesAllSounds, middle, filesSounds2, 0, filesSounds2.length);

        long start = System.currentTimeMillis();

        // Создаём и запускаем первый поток
        ChangeQualitySound soundProcessor1 = new ChangeQualitySound(pathToDstFolder, filesSounds1, start);
        soundProcessor1.start();

        // Создаём и запускаем второй поток
        ChangeQualitySound soundProcessor2 = new ChangeQualitySound(pathToDstFolder, filesSounds2, start);
        soundProcessor2.start();

        // Ожидаем завершения обоих потоков
        try {
            soundProcessor1.join();
            soundProcessor2.join();
        } catch (InterruptedException e) {
            System.err.println("Ошибка при ожидании потоков: " + e.getMessage());
        }

        System.out.println("Все аудиофайлы обработаны!");
    }
}

/*
TODO
    Программа убрала каждый второй сэмпл,
    но время между оставшимися сэмплами увеличила в 2 раза.
    Почему звук по длине не меняется:
    длительность =
    количество сэмплов(в исходной мелодии 44_100, в новой - 22_050) /
    частота дискретизации(в исходной мелодии 44_100 Гц, в новой - 22_050 Гц).

 */
class ChangeQualitySound extends Thread {
    String pathToDstFolder;
    File[] filesAllSounds;
    long start;

    public ChangeQualitySound(String pathToDstFolder, File[] filesAllSounds, long start) {
        this.pathToDstFolder = pathToDstFolder;
        this.filesAllSounds = filesAllSounds;
        this.start = start;
    }

    @Override
    public void run() {
        //TODO Обработка исключений
        try {
            for (File currentFileSound : filesAllSounds) {
                AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(currentFileSound);
                /*
                TODO
                 Как часто нужно "смотреть" на звук (частота).
                 Сколько места занимает одно измерение (разрядность).
                 Это для одного уха или для двух (каналы).
                 originAudioFormat описывает,
                 что измерять звук нужно 44_100 раз в секунду
                 */
                AudioFormat originAudioFormat = originalAudioInputStream.getFormat();

                /*
                TODO
                 Новый формат с половинной частотой.
                 За счёт newAudioFormat "новая" мелодия
                 будет иметь размер примерно в 2 раза меньше,
                 но длиться столько же по времени,
                 сколько и исходная.
                 newAudioFormat описывает,
                 что измерять звук нужно 22_050 раз в секунду
                 */
                AudioFormat newAudioFormat = new AudioFormat(
                        originAudioFormat.getEncoding(),      //Тип кодирования
                        /*
                        TODO
                            Частота дискретизации.
                            Частота дискретизации для звука — это то же самое, что частота кадров для видео.
                            Cколько отдельных измерений в секунду.
                            Меняем частоту: было 44_100, стало 22_050.
                            Для стерео звука одно измерение —
                            это одно число для левого уха + одно число для правого уха.
                         */
                        originAudioFormat.getSampleRate() / 2,
                        originAudioFormat.getSampleSizeInBits(), //Разрядность (бит на сэмпл)
                        originAudioFormat.getChannels(), //Количество каналов
                        originAudioFormat.getFrameSize(), //Размер фрейма в байтах
                        /*
                        TODO
                            Частота фреймов.
                            Меняем количество измерений в секунду: тоже в 2 раза меньше.
                            Это сколько наборов измерений в секунду.
                         */
                        originAudioFormat.getFrameRate() / 2,
                        originAudioFormat.isBigEndian() //Порядок байтов
                );

                /*
                TODO
                    Преобразование (упрощённо: прореживание сэмплов).
                    Читает оригинальные сэмплы;
                    Отбрасывает каждый второй (прореживание);
                    Создаёт новый поток с изменённым форматом
                    //Оригинальные сэмплы (каждый)
                    for (int i = 0; i < originalSamples.length; i++) {
                        newSamples[i] = originalSamples[i];
                    }
                    //Уменьшенная частота (каждый второй)
                    for (int i = 0; i < newSamples.length; i++) {
                        newSamples[i] = originalSamples[i * 2];  // берём с шагом 2
                    }
                    */
                AudioInputStream converted = AudioSystem.getAudioInputStream(newAudioFormat, originalAudioInputStream);

                File output = new File(pathToDstFolder + "/" + currentFileSound.getName());
                AudioSystem.write(converted, AudioFileFormat.Type.WAVE, output);
            }
            long end = System.currentTimeMillis();
            long difference = end - start;
            System.out.println("Time work program: " + difference);
        } catch (Exception ex) {
            ex.getMessage();
        }
    }
}
