import { Component, OnInit } from '@angular/core';
import {ImageUploadService} from "../../services/image-upload.service";
import {firstValueFrom} from "rxjs";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-upload-image',
  templateUrl: './upload-image.component.html',
  styleUrls: ['./upload-image.component.scss'],
  imports: [
    FormsModule
  ],
  standalone: true
})
export class UploadImageComponent  implements OnInit {

  profileInfos = {
    name: 'first',
    photos: []
  };
  selectedFiles: File[] | null = null;

  constructor(private imageUploadService: ImageUploadService) {}

  ngOnInit(): void {
    // Fetch existing profile info if needed
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if(input.files && input.files.length > 0) {
      this.selectedFiles = Array.from(input.files);
    }
  }

  async uploadImage(): Promise<void> {
    if (this.selectedFiles) {
      for (let selectedFile of this.selectedFiles) {
        let result = await firstValueFrom(this.imageUploadService.uploadImage(selectedFile));
        console.log(result)
        // this.profileInfos.photos.push(result); // Add new image URL to profile photos
      }
      this.selectedFiles = null; // Clear the file selection
    }
  }

  immediateUpload(): void {
    if (this.selectedFiles) {
      for (let selectedFile of this.selectedFiles) {
        this.imageUploadService.uploadImage(selectedFile);
      }
      this.selectedFiles = null; // Clear the file selection
    }
  }
}

