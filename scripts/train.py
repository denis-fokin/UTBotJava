import argparse

import pandas as pd
import os
import numpy as np
import sklearn.model_selection
import torch
import json
import sys

from torch.optim.lr_scheduler import ReduceLROnPlateau
from torch.utils.data import TensorDataset, DataLoader
from sklearn.preprocessing import normalize, StandardScaler
from sklearn.metrics import mean_squared_error, mean_absolute_error


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--features_dir", dest='features_dir', type=str)
    parser.add_argument("--output_dir", dest='output_dir', type=str)
    parser.add_argument("--prog_list", dest='prog_list', type=str)
    parser.add_argument("--epochs", default=-1, type=int)
    parser.add_argument("--device", default='gpu', type=str)
    return parser.parse_args()

def get_free_gpu():
    os.system('nvidia-smi -q -d Memory |grep -A4 GPU|grep Free >tmp')
    memory_available = [int(x.split()[2]) for x in open('tmp', 'r').readlines()]
    return np.argmax(memory_available)


args = get_args()
args.device = 'cpu' if args.device == 'cpu' else f'cuda:{get_free_gpu()}'

DATA_DIR = args.features_dir
target_col = 'reward'
feature_cols = ['stack', 'successor', 'testCase', 'coverageByBranch', 'coverageByPath', 'depth', 'cpicnt', 'icnt',
                'covNew', 'subpath1', 'subpath2', 'subpath4', 'subpath8']
progs = []
with open(args.prog_list, 'r') as f:
    for line in f:
        progs.append(line[:-1])

total_df = None
for selector in os.listdir(DATA_DIR):
    for prog in progs:
        if not os.path.exists(os.path.join(DATA_DIR, selector, prog)):
            continue
        for f in os.listdir(os.path.join(DATA_DIR, selector, prog)):
            df = pd.read_csv(os.path.join(DATA_DIR, selector, prog, f))

            if total_df is None:
                total_df = df
            else:
                total_df = pd.concat([total_df, df])
    print(f"Finish collecting data with {selector}", flush=True)

y = np.expand_dims(total_df[target_col].values, axis=1)
x = total_df[feature_cols].values
x = x.astype(float)

scaler = StandardScaler()
scaler = scaler.fit(x)
x = scaler.transform(x)
x_train, x_test, y_train, y_test = sklearn.model_selection.train_test_split(x, y, test_size=0.2)


class linearRegression(torch.nn.Module):
    def __init__(self):
        super(linearRegression, self).__init__()
        self.nn = torch.nn.Sequential(
            torch.nn.Linear(13, 64),
            torch.nn.ReLU(),
            torch.nn.Linear(64, 64),
            torch.nn.ReLU(),
            torch.nn.Linear(64, 1)
        )

    def forward(self, x):
        out = self.nn(x)
        return out



model = linearRegression()
model = model.to(device=args.device)

learningRate = 1e-3
epochs = sys.maxsize if args.epochs == -1 else args.epochs
criterion = torch.nn.MSELoss()
optimizer = torch.optim.Adam(model.parameters(), lr=learningRate, weight_decay=1e-5)
sheduler = ReduceLROnPlateau(optimizer, 'min', patience=3, verbose = True)

tensor_x = torch.Tensor(x_train)
tensor_y = torch.Tensor(y_train)
tensor_x_test = torch.Tensor(x_test)
tensor_y_test = torch.Tensor(y_test)

train_dataloader = DataLoader(TensorDataset(tensor_x, tensor_y), batch_size=64)
test_dataloader = DataLoader(TensorDataset(tensor_x_test, tensor_y_test), batch_size=64)


def train_epoch(model, data_loader, loss_fn, optimizer, device):
    model.train(True)

    running_loss = 0.0
    processed_data = 0

    for batch_x, batch_y in data_loader:
        batch_x = batch_x.to(device=device)
        batch_y = batch_y.to(device=device)
        optimizer.zero_grad()

        outputs = model(batch_x)
        loss = loss_fn(outputs, batch_y)

        running_loss += loss.detach().cpu().item()
        processed_data += batch_x.shape[0]

        loss.backward()
        optimizer.step()

    return running_loss / processed_data


def eval_epoch(model, data_loader, loss_fn, device):
    model.eval()

    running_loss = 0.0
    running_ae = 0.0
    processed_data = 0

    for batch_x, batch_y in data_loader:
        batch_x = batch_x.to(device=device)
        batch_y = batch_y.to(device=device)
        outputs = model(batch_x)
        loss = loss_fn(outputs, batch_y)

        running_loss += loss.detach().cpu().item()
        processed_data += batch_x.shape[0]
        running_ae += torch.sum(torch.abs(outputs - batch_y))

    return running_loss / processed_data, running_ae / processed_data


for epoch in range(epochs):
    if (optimizer.param_groups[0]['lr'] <= 1e-6): break

    train_loss = train_epoch(model, train_dataloader, criterion, optimizer, args.device)
    val_loss, val_mae = eval_epoch(model, test_dataloader, criterion, args.device)
    sheduler.step(val_loss)

    print('epoch {}, train_loss {}, val_loss {}, val_mae {}'.format(epoch, train_loss, val_loss, val_mae), flush=True)

if not os.path.exists(args.output_dir):
    os.makedirs(args.output_dir)

with open(os.path.join(args.output_dir, 'scaler.txt'), 'w') as f:
    for array in np.vstack((scaler.mean_, scaler.scale_)).tolist():
        for item in array[:-1]:
            f.write("%s," % item)
        f.write("%s" % array[-1])
        f.write("\n")

nn = {
    "linearLayers": [],
    "activationLayers": ["reLU", "reLU", "reLU"],
    "biases": [],
}

state_dict = model.cpu().state_dict()
for i in [0, 2, 4]:
    nn["linearLayers"] += [state_dict[f'nn.{i}.weight'].numpy().tolist()]
    nn["biases"] += [state_dict[f'nn.{i}.bias'].numpy().tolist()]

with open(os.path.join(args.output_dir, 'nn.json'), 'w') as f:
    json.dump(nn, f, indent=4)
