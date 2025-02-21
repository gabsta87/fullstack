import {Component, OnInit} from '@angular/core';
import {NgForOf, NgOptimizedImage} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import { CommonModule } from '@angular/common';
import {UploadImageComponent} from "../upload-image/upload-image.component";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    NgForOf,
    NgOptimizedImage,
    IonicModule,
    CommonModule,
    UploadImageComponent,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {

  profileSimple !: ProfileDetail;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.profileSimple = this.route.snapshot.data['profile'];
    console.log(this.profileSimple);
  }
}

// This class is used in the gallery, to have minimal information on all profiles
export class Profile{
  id!: number;
  pseudo!:string;
  description!:string;
  phone!:string;
  address!:string;
  comments!:string[];
  mainPhoto!:string;
}

// This class is used for a profile detailed page, with all photos loaded
export class ProfileDetail extends Profile{
  photos!:string[];
}
