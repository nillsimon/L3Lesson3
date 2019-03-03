package kz.maxsoft.l3lesson3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    static final int PICK_REQUEST = 1;
    String filesDir;
    final String picIn = "pic.jpg";
    final String picOut = "pic.png";
    Button loadButton;
    Button convertButton;
    ImageView imageView;
    ProgressBar pb;
    ProgressBar convertPB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        filesDir = getApplicationContext().getFilesDir().toString();

        pb = findViewById(R.id.pb);
        pb.setVisibility(View.GONE);

        convertPB = findViewById(R.id.convertPB);
        convertPB.setVisibility(View.GONE);

        loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, PICK_REQUEST);
            convertButton.setVisibility(View.GONE);
        });

        convertButton = findViewById(R.id.convertButton);
        convertButton.setVisibility(View.GONE);
        convertButton.setOnClickListener(v -> {
            convertButton.setVisibility(View.INVISIBLE);
            convertPB.setVisibility(View.VISIBLE);
            Observable<String> observable = Observable.create(emitter -> {
                try {
                    File file = new File(filesDir, picIn);
                    File filePNG = new File(filesDir, picOut);
                    if (file.exists()) {
                        Log.d(TAG, "file JPG exists");
                        // конвертировать
                        Log.d(TAG, "converting...");
                        OutputStream outputStream = new FileOutputStream(filePNG);
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                        outputStream.flush();
                        outputStream.close();
                        Log.d(TAG, "...converted");
                    }
                    if (filePNG.exists()) {
                        Log.d(TAG, "file PNG exists");
                        emitter.onNext(picOut);
                    } else {
                        emitter.onNext("");
                    }

                } catch (Exception e) {
                    emitter.onError(e);
                }
            });

            observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onNext(String s) {
                            Log.d(TAG, "--- filename " + s);
                            convertButton.setVisibility(View.VISIBLE);
                            convertPB.setVisibility(View.GONE);
                            if (!s.equals(""))
                                Toast.makeText(MainActivity.this, "Конвертирование выполнено", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(MainActivity.this, "Конвертирование не выполнено", Toast.LENGTH_SHORT).show();
                        }

                        @Override public void onComplete() {

                        }

                        @Override
                        public void onError(Throwable t) {
                            Log.d(TAG, "Error.", t);
                        }

                        @Override
                        public void onSubscribe(Disposable d) {

                        }
                    });

        });
    }

    public void saveBitmapFile(Bitmap bitmap) {
        loadButton.setVisibility(View.INVISIBLE);
        convertButton.setVisibility(View.INVISIBLE);
        pb.setVisibility(View.VISIBLE);
        Observable<String> observable = Observable.create(emitter -> {
            try {
                File file = new File(filesDir, picIn);
                if (file.exists()) {
                    boolean res = file.delete();
                    if (res)
                        file = new File(filesDir, picIn);
                }
                try {
                    Log.d(TAG, "--- loading... ");
                    OutputStream outStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream);
                    outStream.flush();
                    outStream.close();
                    Log.d(TAG, "--- ...loaded " + picIn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                emitter.onNext(picIn);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });

        observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "--- filename " + s);
                        pb.setVisibility(View.GONE);
                        loadButton.setVisibility(View.VISIBLE);
                        convertButton.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "Файл сохранен", Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.d(TAG, "Error.", t);
                    }

                    @Override
                    public void onSubscribe(Disposable d) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Bitmap bitmap = null;
        imageView = findViewById(R.id.imageView);

        switch(requestCode) {
            case PICK_REQUEST:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // сохранить bitmap в jpg
                    saveBitmapFile(bitmap);

                    Picasso.with(this)
                            .load(selectedImage)
                            .fit()
                            .centerCrop()
                            .into(imageView);
                }
        }
    }
}
