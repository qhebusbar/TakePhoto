package org.devio.takephoto.permission;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.devio.takephoto.app.TakePhoto;
import org.devio.takephoto.model.InvokeParam;
import org.devio.takephoto.model.TContextWrap;
import org.devio.takephoto.uitl.TConstant;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by penn on 16/9/22.
 */
public class PermissionManager {
    public enum TPermission {
        STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        CAMERA(Manifest.permission.CAMERA),
        READ_MEDIA_IMAGES(Manifest.permission.READ_MEDIA_IMAGES),
        READ_MEDIA_AUDIO(Manifest.permission.READ_MEDIA_AUDIO),
        READ_MEDIA_VIDEO(Manifest.permission.READ_MEDIA_VIDEO);
        String stringValue;

        TPermission(String stringValue) {
            this.stringValue = stringValue;
        }

        public String stringValue() {
            return stringValue;
        }
    }


    public enum TPermissionType {
        GRANTED("已授权"), DENIED("未授权"), WAIT("等待授权"), NOT_NEED("无需授权"), ONLY_CAMERA_DENIED("没有拍照权限"), ONLY_STORAGE_DENIED("没有读写SD卡权限");
        String stringValue;

        TPermissionType(String stringValue) {
            this.stringValue = stringValue;
        }

        public String stringValue() {
            return stringValue;
        }
    }


    private final static String[] methodNames =
            {"onPickFromCapture", "onPickFromCaptureWithCrop", "onPickMultiple", "onPickMultipleWithCrop", "onPickFromDocuments",
                    "onPickFromDocumentsWithCrop", "onPickFromGallery", "onPickFromGalleryWithCrop", "onCrop"};

    /**
     * 检查当前应用是否被授予相应权限
     *
     * @param contextWrap
     * @param method
     * @return
     */
    public static TPermissionType checkPermission(@NonNull TContextWrap contextWrap, @NonNull Method method) {
        String methodName = method.getName();
        boolean contain = false;
        for (int i = 0, j = methodNames.length; i < j; i++) {
            if (TextUtils.equals(methodName, methodNames[i])) {
                contain = true;
                break;
            }
        }
        if (!contain) {
            return TPermissionType.NOT_NEED;
        }


        boolean cameraGranted = true;
        boolean storageGranted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean storageGranted1 = ContextCompat.checkSelfPermission(contextWrap.getActivity(), TPermission.READ_MEDIA_IMAGES.stringValue())
                    == PackageManager.PERMISSION_GRANTED;
            boolean storageGranted2 = ContextCompat.checkSelfPermission(contextWrap.getActivity(), TPermission.READ_MEDIA_AUDIO.stringValue())
                    == PackageManager.PERMISSION_GRANTED;
            boolean storageGranted3 = ContextCompat.checkSelfPermission(contextWrap.getActivity(), TPermission.READ_MEDIA_VIDEO.stringValue())
                    == PackageManager.PERMISSION_GRANTED;
            storageGranted = storageGranted1 && storageGranted2 && storageGranted3;

        } else {
            storageGranted = ContextCompat.checkSelfPermission(contextWrap.getActivity(), TPermission.STORAGE.stringValue())
                    == PackageManager.PERMISSION_GRANTED;

        }

        if (TextUtils.equals(methodName, "onPickFromCapture") || TextUtils.equals(methodName, "onPickFromCaptureWithCrop")) {
            cameraGranted = ContextCompat.checkSelfPermission(contextWrap.getActivity(), TPermission.CAMERA.stringValue())
                    == PackageManager.PERMISSION_GRANTED;
        }


        boolean granted = storageGranted && cameraGranted;
        if (!granted) {
            ArrayList<String> permissions = new ArrayList<>();
            if (!storageGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(TPermission.READ_MEDIA_IMAGES.stringValue());
                    permissions.add(TPermission.READ_MEDIA_AUDIO.stringValue());
                    permissions.add(TPermission.READ_MEDIA_VIDEO.stringValue());

                } else {
                    permissions.add(TPermission.STORAGE.stringValue());
                }

            }
            if (!cameraGranted) {
                permissions.add(TPermission.CAMERA.stringValue());
            }
            requestPermission(contextWrap, permissions.toArray(new String[permissions.size()]));
        }
        return granted ? TPermissionType.GRANTED : TPermissionType.WAIT;
    }

    public static void requestPermission(@NonNull TContextWrap contextWrap, @NonNull String[] permissions) {
        if (contextWrap.getFragment() != null) {
            contextWrap.getFragment().requestPermissions(permissions, TConstant.PERMISSION_REQUEST_TAKE_PHOTO);
        } else {
            ActivityCompat.requestPermissions(contextWrap.getActivity(), permissions, TConstant.PERMISSION_REQUEST_TAKE_PHOTO);
        }
    }

    public static TPermissionType onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == TConstant.PERMISSION_REQUEST_TAKE_PHOTO) {
            boolean cameraGranted = true,
                    storageGranted = true;
            for (int i = 0, j = permissions.length; i < j; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    boolean storageGranted1 = true, storageGranted2 = true, storageGranted3 = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (TextUtils.equals(TPermission.READ_MEDIA_IMAGES.stringValue(), permissions[i])) {
                            storageGranted1 = false;

                        } else if (TextUtils.equals(TPermission.READ_MEDIA_AUDIO.stringValue(), permissions[i])) {
                            storageGranted2 = false;
                        } else if (TextUtils.equals(TPermission.READ_MEDIA_VIDEO.stringValue(), permissions[i])) {
                            storageGranted3 = false;
                        }
                        storageGranted = storageGranted1 && storageGranted2 && storageGranted3;
                    } else {
                        if (TextUtils.equals(TPermission.STORAGE.stringValue(), permissions[i])) {
                            storageGranted = false;
                        } else if (TextUtils.equals(TPermission.CAMERA.stringValue(), permissions[i])) {
                            cameraGranted = false;
                        }
                    }

                }
            }
            if (cameraGranted && storageGranted) {
                return TPermissionType.GRANTED;
            }
            if (!cameraGranted && storageGranted) {
                return TPermissionType.ONLY_CAMERA_DENIED;
            }
            if (!storageGranted && cameraGranted) {
                return TPermissionType.ONLY_STORAGE_DENIED;
            }
            if (!storageGranted && !cameraGranted) {
                return TPermissionType.DENIED;
            }
        }
        return TPermissionType.WAIT;
    }

    public static void handlePermissionsResult(Activity activity, TPermissionType type, InvokeParam invokeParam,
                                               TakePhoto.TakeResultListener listener) {
        String tip = null;
        switch (type) {
            case DENIED:
                listener.takeFail(null, tip = activity.getResources().getString(org.devio.takephoto.R.string.tip_permission_camera_storage));
                break;
            case ONLY_CAMERA_DENIED:
                listener.takeFail(null, tip = activity.getResources().getString(org.devio.takephoto.R.string.tip_permission_camera));
                break;
            case ONLY_STORAGE_DENIED:
                listener.takeFail(null, tip = activity.getResources().getString(org.devio.takephoto.R.string.tip_permission_storage));
                break;
            case GRANTED:
                try {
                    invokeParam.getMethod().invoke(invokeParam.getProxy(), invokeParam.getArgs());
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.takeFail(null, tip = activity.getResources().getString(org.devio.takephoto.R.string.tip_permission_camera_storage));
                }
                break;
            default:
                break;
        }
        if (tip != null) {
            Toast.makeText(activity, tip, Toast.LENGTH_LONG).show();
        }

    }
}
