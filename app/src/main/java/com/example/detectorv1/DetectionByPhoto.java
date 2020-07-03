package com.example.detectorv1;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.detectorv1.tfliteAPI.Classifier;
import com.example.detectorv1.tfliteAPI.TFLiteObjectDetectionAPIModel;

import java.util.List;

import static java.lang.StrictMath.min;


public class DetectionByPhoto extends AppCompatActivity {
    private static int RESULT_LOAD_IMAGE = 1;
    Classifier detector;
    private static final int INPUT_SIZE = 300;
    private static final double MINIMUM_CONFIDENCE = 0.6;
    private static final boolean IS_QUANTIZED = true;
    private boolean imageLoaded = false, imageDetected = false;
    private static final String MODEL_FILE = "detect.tflite";
    private static final String LABELS_FILE = "file:///android_asset/labelmap.txt";
    private Drawable prevDrawable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_by_photo);
        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadImage);
        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        Button buttonInfo = (Button) findViewById(R.id.buttonInfo);
        final ImageView imageView = (ImageView) findViewById(R.id.image);


        try {
            detector = TFLiteObjectDetectionAPIModel.create(getAssets(), MODEL_FILE, LABELS_FILE, INPUT_SIZE, IS_QUANTIZED);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);

            }
        });

        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (imageView.getDrawable() != prevDrawable) {
                    imageLoaded = true;
                    imageDetected = false;

                }

                if (imageLoaded && !imageDetected) {
                    Bitmap image = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                    Bitmap imageMutable = image.copy(Bitmap.Config.ARGB_8888, true);
                    int prevHeight = image.getHeight();
                    int prevWidth = image.getWidth();
                    Bitmap scaledImage = Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, true);
                    final List<Classifier.Recognition> results = detector.recognizeImage(scaledImage);

                    final Canvas canvas = new Canvas(imageMutable);
                    final Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStyle(Paint.Style.STROKE);
                    float lineWidth = (min(prevHeight, prevWidth) / 60.0f);
                    float textWidth = (min(prevHeight, prevWidth) / 100.0f);

                    int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.rgb(255, 128, 0), Color.rgb(255, 102, 178), Color.rgb(0, 0, 0), Color.rgb(127, 0, 255), Color.rgb(102, 102, 0)};


                    for (final Classifier.Recognition result : results) {
                        final RectF location = result.getLocation();

                        if (result.getLocation() != null && result.getConfidence() >= MINIMUM_CONFIDENCE) {
                            paint.setStrokeWidth(lineWidth);
                            paint.setColor(colors[Integer.parseInt(result.getId())]);
                            location.left = (int) (location.left * ((double) prevWidth / INPUT_SIZE));
                            location.right = (int) (location.right * ((double) prevWidth / INPUT_SIZE));
                            location.top = (int) (location.top * ((double) prevHeight / INPUT_SIZE));
                            location.bottom = (int) (location.bottom * ((double) prevHeight / INPUT_SIZE));
                            canvas.drawRect(location, paint);
                            paint.setStrokeWidth(textWidth / 3);
                            paint.setTextSize(textWidth * 4);
                            canvas.drawText(result.toString(), location.left, location.top - lineWidth, paint);
                        }
                    }
                    imageView.setImageBitmap(imageMutable);
                    prevDrawable = imageView.getDrawable();
                    imageDetected = true;
                } else {
                    Toast toast = null;
                    if (imageDetected)
                        toast = Toast.makeText(getApplicationContext(), "This image is already detected. Please, choose another image!", Toast.LENGTH_SHORT);
                    if (!imageLoaded)
                        toast = Toast.makeText(getApplicationContext(), "Please, choose image!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        buttonInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast1 =
                        Toast.makeText(
                                getApplicationContext(), "This app is using SSD_Mobilenet.", Toast.LENGTH_SHORT);

                final Toast toast2 =
                        Toast.makeText(
                                getApplicationContext(), "Note: Results of detection can be displayed incorrectly if using images of small resolution(less then 300*300 px).", Toast.LENGTH_LONG);

                toast1.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toast2.show();
                    }
                }, 2000);


            }
        });
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //This functions return the selected image from gallery
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.image);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            //Setting the URI so we can read the Bitmap from the image
            imageView.setImageURI(null);
            imageView.setImageURI(selectedImage);


        }


    }


}