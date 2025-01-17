
package com.example.ddl.ui.hochladen;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.ddl.Globals;
import com.example.ddl.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class HochladenFragment extends Fragment {


    private Button hochladen;
    private Button zuruecksetzen;
    private Button auswahl;
    private Button speichern;
    public  static ImageView vorschaubild;



    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_hochladen, container, false);
        hochladen = root.findViewById(R.id.hochladen);
        zuruecksetzen = root.findViewById(R.id.zuruecksetzen);
        auswahl = root.findViewById(R.id.auswahl);
        speichern = root.findViewById(R.id.speichern);
        vorschaubild =root.findViewById(R.id.vorschaubild);

        Toast toast2 = Toast.makeText(getActivity(),
                "Hochgeladen!", Toast.LENGTH_SHORT);
        toast2.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 300);

        Toast toast3 = Toast.makeText(getActivity(),
                "Hochladen fehlgeschlagen!", Toast.LENGTH_SHORT);
        toast3.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 300);

        Handler handler = new Handler();
        LoadingDialog loadingDialog2 = new LoadingDialog();


        auswahl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage(getContext());
            }
        });

        zuruecksetzen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vorschaubild.setImageResource(R.drawable.stern);
                Globals.reset=true;
                Toast toast = Toast.makeText(getActivity(),
                        "Zurückgesetzt!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 300);
                toast.show();
            }
        });



        speichern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vorschaubild.setDrawingCacheEnabled(true);
                vorschaubild.buildDrawingCache();
                Globals.galerie[Globals.gcount] = Bitmap.createBitmap(vorschaubild.getDrawingCache());
                vorschaubild.setDrawingCacheEnabled(false);
                saveToInternalStorage(Globals.galerie[Globals.gcount]);
                Toast toast = Toast.makeText(getActivity(),
                        "Zu Favoriten hinzugefügt!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 300);
                toast.show();

                if (Globals.gcount < 7) {
                    Globals.gcount++;
                }
                else {
                    Globals.gcount=0;
                }

            }
        });

        hochladen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                vorschaubild.setDrawingCacheEnabled(true);
                vorschaubild.buildDrawingCache();


                Globals.upload = Bitmap.createBitmap(vorschaubild.getDrawingCache());
                vorschaubild.setDrawingCacheEnabled(false);
                saveToInternalStorage2(Globals.upload);

                UploadFile test = new UploadFile(HochladenFragment.this, handler, toast2, toast3, loadingDialog2);
                test.execute();




            }
        });

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (Globals.firstVisit){
            Globals.firstVisit=false;
        }
        else {
            if (Globals.vorschau != null && !Globals.reset) {
                vorschaubild.setImageBitmap(Globals.vorschau);
            }
        }
    }



    public void selectImage(Context context) {
        final CharSequence[] options = { "Kamera", "Galerie","Zurück" };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Motiv auswählen");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Kamera")) {
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    File photoFile = null;
                    try {
                        photoFile = createCameraFile();
                    } catch (IOException ex) {

                    }
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(getActivity(),
                                "com.example.android.fileprovider",
                                photoFile);
                        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePicture, 0);
                    }


                } else if (options[item].equals("Galerie")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto , 1);

                } else if (options[item].equals("Zurück")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data == null) {
                        //Globals.vorschau = (Bitmap) data.getExtras().get("data");
                        loadImageFromStorage(Globals.cameraPath);
                        vorschaubild.setImageBitmap(Globals.vorschau);
                        Globals.reset=false;
                    }
                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage =  data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContext().getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                Globals.vorschau=BitmapFactory.decodeFile(picturePath);
                                vorschaubild.setImageBitmap(Globals.vorschau);
                                Globals.reset=false;
                                cursor.close();
                            }
                        }
                    }
                    break;
            }
        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath=new File(directory,"bild"+Globals.gcount+".png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directory.getAbsolutePath();
    }

    private String saveToInternalStorage2(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath=new File(directory,"upload.png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directory.getAbsolutePath();
    }




    private File createCameraFile() throws IOException {
        // Create an image file name

        String imageFileName = "camera";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        Globals.cameraPath = image.getAbsolutePath();
        return image;
    }

    private void loadImageFromStorage(String path)
    {
        try {
            File f= new File(path);
            Globals.vorschau= BitmapFactory.decodeStream(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }





}