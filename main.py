from fastapi import FastAPI, Response, status, HTTPException
from fastapi.params import Body
from os import environ as env
from pydantic import BaseModel
from random import randrange
import firebase_admin
from firebase_admin import credentials, initialize_app, db, messaging
import logging
from typing import Union
import bcrypt
import json

# import pyrebase

# /*
#     * Code attributions:
#     *
#     * Auther: freeCodeCamp.org; Python API Development - Comprehensive Course for Beginners: link- https://youtu.be/0sOvCWFmrtA
#     * Auther: Phillipp Lackner; Local Notifications in Android - The Full Guide (Android Studio Tutorial) : link - https://youtu.be/LP623htmWcI
#     * Auther: Code With Cal; Daily Calendar View Android Studio Tutorial : link - https://youtu.be/Aig99t-gNqM
#     * Auther: Foxandroid; How to Add SearchView in Android App using Kotlin | SearchView | Kotlin | Android studio : link - https://youtu.be/oE8nZRJ9vxA
#     *
#     * AI Tools
#     * Gemini
#     * ChatGpt
#     * Amazon Q
#     * CoPilot
#     *
#     * */

app = FastAPI()

# Initialize Firebase Admin SDK
cred = credentials.Certificate("./serviceAccountKey.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://opsc7312database-default-rtdb.firebaseio.com'
})

class Users(BaseModel):
    __tablename__ = "users"

    name: str
    surname: str
    email: str
    phone: str
    username: str
    password: str

class Appointments(BaseModel):
    __tablename__ = "appointments"

    doctor: str
    patientname: str
    date: str
    time: str
    description: str
    cancel_booking: bool

class Dentists(BaseModel):
    __tablename__ = "dentists"

    name: str
    surname: str
    email: str
    phone: str
    address: str
    username: str
    password: str

# cred = credentials.Certificate("serviceAccountKey.json")
# firebase_admin.initialize_app(cred)

def hash_password(password: str) -> str:
    salt = bcrypt.gensalt()
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed_password.decode('utf-8')

@app.get("/")
async def root():
    return {"message": f"Hello World! Secret = {env['MY_VARIABLE']}"}

my_appointments = [{"doctor": "Doctor", "patientname": "Chris Phillips", "date": "Date", "time": "Time", "description": "Description", "id": 1},
                   {"doctor": "Dr. Bhika", "patientname": "Pearl Thusi", "date": "19/09/2024", "time": "15:00", 
                    "description": "Wisdom teeth aching", "cancel_booking": False, "id": 2}]

my_users = [{"name": "Thabo", "surname": "Jobe", "email": "jobe@jobe.com", "phonenumber": "0828899123", 
             "username": "Jobe_7", "password": "#!Jobe123", "id": 1}, {"name": "Claire", "surname": "Gill", 
            "email": "clairegill@gmail.com", "phonenumber": "0762339898", "username": "LadyFish", 
            "password": "FoundNemo", "id": 2}]

my_dentists = [{"name": "Sandesh", "surname": "Bhika", "email": "drbhika@gp.com", "phonenumber": "0828899123","address": "728 Berk Crescent, Sandton",
                "username": "DrBhika", "password": "#!Bhika77", "id": 1}]

def find_appointment(id):
    for p in my_appointments:
        if p["id"] == id:
            return p
        
def find_user(id):
    for u in my_users:
        if u["id"] == id:
            return u
        
def find_dentist(id):
    for d in my_dentists:
        if d["id"] == id:
            return d

def find_index_user(id):
    for i, p in enumerate(my_users):
        if p['id'] == id:
            return i
        
def find_index_appointment(id):
    for i, p in enumerate(my_appointments):
        if p['id'] == id:
            return i
        

async def send_push_notification(title: str, body: str):
    message = messaging.Message(
        notification=messaging.Notification(
            title=title,
            body=body
        ),
        topic="all",
    )
    response = messaging.send(message)
    print('Successfully sent message:', response)
    

#################################################################################################################

# Create user
@app.post('/signup', status_code=status.HTTP_201_CREATED)
async def create_an_account(user: Users):
    user_dict = user.dict()
    user_dict['id'] = randrange(0, 10000000)
    my_users.append(user_dict)
    user_dict['password'] = hash_password(user.password)

    ref = db.reference('users')
    new_user_ref = ref.push()
    #new_user_ref = ref.child(user_dict['id'])
    new_user_ref.set(user_dict)

    await send_push_notification("New User", f"A new user with ID {user_dict['id']} has signed up.")

    return {"new_user": user_dict}

# Login user
@app.post('/login')
async def create_access_token():
    pass

# @app.post('/ping')
# async def validate_token():
#     pass

# Get Users
@app.get('/users')
async def get_users():
    ref = db.reference('users')
    users = ref.get()
    return {"data": users}

# Get User
@app.get('/users/{id}')
async def get_user(id: Union[int, str], response: Response):
    # user = find_user(id)
    ref_id = str(id)
    logging.info(f'Checking user at path: users/{ref_id}')
    ref = db.reference(f'users/{ref_id}')
    user = ref.get()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, 
                            detail=f"user with id: {ref_id} was not found")
    return {"user_detail": user}

# Delete User
@app.delete("/users/{id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_user(id: Union[int, str]):
    #index = find_index_user(id)
    ref_id = str(id)
    ref = db.reference(f'users/{ref_id}')

    if ref.get() is None:
        raise HTTPException(status_code=404, detail="User not found")
    ref.delete()

    await send_push_notification("User Deleted", f"User with ID {ref_id} has been deleted.")

    return Response(status_code=status.HTTP_204_NO_CONTENT)

# Update User
@app.put("/users/{id}")
async def update_user(id: Union[int, str], user: Users):
    #index = find_index_user(id)
    ref_id = str(id)
    ref = db.reference(f'users/{ref_id}')
    logging.info(f'Checking user at path: users/{ref_id}')

    if ref.get() is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    user_dict = user.dict()
    user_dict['id'] = id
    ref.update(user_dict)

    await send_push_notification("User Updated", f"User with ID {ref_id} has been updated.")

    return {'data': user_dict}

###############################################################################################################

# Get Dentists
@app.get('/dentists')
async def get_dentists():
    ref = db.reference('dentists')
    dentists = ref.get()
    return {"data": dentists}

# Create Dentist
@app.post('/dentists', status_code=status.HTTP_201_CREATED)
async def create_dentist(dentist: Dentists):
    dentist_dict = dentist.dict()
    dentist_dict['id'] = randrange(0, 10000000)
    dentist_dict['password'] = hash_password(dentist.password)
    my_users.append(dentist_dict)

    ref = db.reference('dentists')
    new_dentist_ref = ref.push()
    new_dentist_ref.set(dentist_dict)

    await send_push_notification("New Dentist Added", f"A new dentist, {dentist.name} {dentist.surname}, has been added.")

    return {"new_user": dentist_dict}

# Get Dentist
@app.get('/dentists/{id}')
async def get_dentist(id: Union[int, str], response: Response):
    ref_id = str(id)
    ref = db.reference(f'dentists/{ref_id}')
    dentist = ref.get()
    if not dentist:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND,
                            detail=f"dentist with id: {ref_id} was not found")
    return {"dentist_detail": dentist}

# Delete Dentist
@app.delete("/dentists/{id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_dentist(id: Union[int, str]):
    ref_id = str(id)
    ref = db.reference(f'dentists/{ref_id}')

    if ref.get() is None:
        raise HTTPException(status_code=404, detail="User not found")
    ref.delete()

    await send_push_notification("Dentist Deleted", f"Dentist with ID {ref_id} has been deleted.")

    return Response(status_code=status.HTTP_204_NO_CONTENT)

# Update Dentist
@app.put("/dentists/{id}")
async def update_dentist(id: Union[int, str], dentist: Dentists):
    ref_id = str(id)
    ref = db.reference(f'dentists/{ref_id}')
    logging.info(f'Checking dentist at path: dentists/{ref_id}')

    if ref.get() is None:
        raise HTTPException(status_code=404, detail="User not found")

    dentist_dict = dentist.dict()
    dentist_dict['id'] = id
    ref.update(dentist_dict)

    await send_push_notification("Dentist Updated", f"Dentist with ID {ref_id} has been updated.")

    return {'data': dentist_dict}

###############################################################################################################

# Get Appointment
@app.get('/appointments')
async def get_appointments():
    ref = db.reference('appointments')
    appointments = ref.get()
    return {"data": appointments}

# Get Appointment
@app.get('/appointments/{id}')
async def get_appointment(id: Union[int, str], response: Response):
    #appointment = find_appointment(id)
    ref_id = str(id)
    ref = db.reference(f'appointments/{ref_id}')
    appointment = ref.get()
    if not appointment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, 
                            detail=f"appointment with id: {ref_id} was not found")
    return {"appointment_detail": appointment}

# Create Appointment
@app.post('/appointments', status_code=status.HTTP_201_CREATED)
async def create_appointment(appointment: Appointments):
    appointment_dict = appointment.dict()
    appointment_dict['id'] = str(randrange(0, 10000000))

    #my_appointments.append(appointment_dict)
    ref = db.reference('appointments')
    new_appointment_ref = ref.child(appointment_dict['id'])
    new_appointment_ref.set(appointment_dict)

    await send_push_notification("New Appointment", f"You have a new appointment with {appointment_dict['doctor']}")

    return {"data": appointment_dict}

# Delete Appointment
@app.delete("/appointments/{id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_appointment(id: Union[int, str]):
    #index = find_index_appointment(id)
    ref_id = str(id)
    ref = db.reference(f'appointments/{ref_id}')

    if ref.get() is None:
        raise HTTPException(status_code=404, detail="Appointment not found")
    ref.delete()

    await send_push_notification("Appointment Cancelled", f"Appointment with ID {ref_id} has been deleted.")

    return Response(status_code=status.HTTP_204_NO_CONTENT)

# Update Appointment
@app.put("/appointments/{id}")
async def update_appointment(id: Union[int, str], appointment: Appointments):
    #index = find_index_appointment(id)
    ref_id = str(id)
    ref = db.reference(f'appointments/{ref_id}')

    if ref.get() is None:
        raise HTTPException(status_code=404, detail="Appointment not found")
    
    appointment_dict = appointment.dict()
    appointment_dict['id'] = id
    ref.update(appointment_dict)

    await send_push_notification("Appointment Updated", f"Appointment with ID {ref_id} has been updated.")

    return {"data": appointment_dict}

##########################################################################################################